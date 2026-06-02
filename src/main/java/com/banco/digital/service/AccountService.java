package com.banco.digital.service;

import com.banco.digital.dto.AccountRequest;
import com.banco.digital.dto.AccountResponse;
import com.banco.digital.dto.MovementResponse;
import com.banco.digital.entity.Account;
import com.banco.digital.exception.AccountNotFoundException;
import com.banco.digital.repository.AccountRepository;
import com.banco.digital.repository.TransferRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public AccountService(AccountRepository accountRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        return AccountResponse.from(getAccountOrThrow(id));
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        Account account = new Account(request.name(), request.initialBalance());
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> getMovements(Long accountId) {
        getAccountOrThrow(accountId);
        return transferRepository.findMovementsByAccountId(accountId).stream()
                .map(transfer -> MovementResponse.from(transfer, accountId))
                .toList();
    }

    Account getAccountOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
