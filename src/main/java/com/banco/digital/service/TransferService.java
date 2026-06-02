package com.banco.digital.service;

import com.banco.digital.dto.TransferRequest;
import com.banco.digital.dto.TransferResponse;
import com.banco.digital.entity.Account;
import com.banco.digital.entity.Transfer;
import com.banco.digital.entity.TransferStatus;
import com.banco.digital.event.TransferCompletedEvent;
import com.banco.digital.exception.AccountNotFoundException;
import com.banco.digital.exception.InvalidTransferException;
import com.banco.digital.repository.AccountRepository;
import com.banco.digital.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String chaveRetry) {
        if (chaveRetry != null && !chaveRetry.isBlank()) {
            return transferRepository.findByChaveRetry(chaveRetry)
                    .map(TransferResponse::from)
                    .orElseGet(() -> executeTransfer(request, chaveRetry));
        }
        return executeTransfer(request, null);
    }

    private TransferResponse executeTransfer(TransferRequest request, String chaveRetry) {
        validateTransferRequest(request);

        List<Long> orderedIds = List.of(request.sourceAccountId(), request.destinationAccountId())
                .stream()
                .sorted()
                .toList();

        // lock na ordem do id - ordem fixa evita deadlock
        Map<Long, Account> lockedAccounts = accountRepository.findAllByIdForUpdate(orderedIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        Account source = lockedAccounts.get(request.sourceAccountId());
        Account destination = lockedAccounts.get(request.destinationAccountId());

        if (source == null) {
            throw new AccountNotFoundException(request.sourceAccountId());
        }
        if (destination == null) {
            throw new AccountNotFoundException(request.destinationAccountId());
        }

        source.debit(request.amount());
        destination.credit(request.amount());

        accountRepository.save(source);
        accountRepository.save(destination);

        Transfer transfer = new Transfer(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                TransferStatus.COMPLETED
        );
        if (chaveRetry != null && !chaveRetry.isBlank()) {
            transfer.setChaveRetry(chaveRetry);
        }
        transfer = transferRepository.save(transfer);

        eventPublisher.publishEvent(new TransferCompletedEvent(
                transfer.getId(),
                source.getId(),
                destination.getId(),
                transfer.getAmount(),
                source.getName(),
                destination.getName()
        ));

        return TransferResponse.from(transfer);
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new InvalidTransferException("Origem e destino nao podem ser a mesma conta");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Valor tem que ser maior que zero");
        }
    }
}
