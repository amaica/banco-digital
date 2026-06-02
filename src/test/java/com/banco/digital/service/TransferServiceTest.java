package com.banco.digital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banco.digital.dto.TransferRequest;
import com.banco.digital.dto.TransferResponse;
import com.banco.digital.entity.Account;
import com.banco.digital.entity.Transfer;
import com.banco.digital.entity.TransferStatus;
import com.banco.digital.event.TransferCompletedEvent;
import com.banco.digital.exception.AccountNotFoundException;
import com.banco.digital.exception.InsufficientBalanceException;
import com.banco.digital.exception.InvalidTransferException;
import com.banco.digital.repository.AccountRepository;
import com.banco.digital.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        source = new Account("Alice", new BigDecimal("1000.00"));
        destination = new Account("Bruno", new BigDecimal("500.00"));
    }

    @Test
    void shouldTransferSuccessfully() {
        source = accountWithId(1L, source);
        destination = accountWithId(2L, destination);

        when(accountRepository.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(source, destination));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transferWithId(10L, transfer);
            return transfer;
        });

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("200.00"));
        TransferResponse response = transferService.transfer(request, null);

        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(source.getBalance()).isEqualByComparingTo("800.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("700.00");

        ArgumentCaptor<TransferCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TransferCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().amount()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldRejectTransferWhenInsufficientBalance() {
        source = accountWithId(1L, new Account("Alice", new BigDecimal("50.00")));
        destination = accountWithId(2L, destination);

        when(accountRepository.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(source, destination));

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("200.00"));

        assertThatThrownBy(() -> transferService.transfer(request, null))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(transferRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(request, null))
                .isInstanceOf(InvalidTransferException.class)
                .hasMessageContaining("nao podem ser a mesma conta");

        verify(accountRepository, never()).findAllByIdForUpdate(any());
    }

    @Test
    void shouldRejectTransferWhenAccountNotFound() {
        source = accountWithId(1L, source);

        when(accountRepository.findAllByIdForUpdate(List.of(1L, 2L))).thenReturn(List.of(source));

        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(request, null))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Conta não encontrada");
    }

    @Test
    void deveDevolverMesmaTransferenciaQuandoChaveRetryJaExiste() {
        Transfer existing = new Transfer(1L, 2L, new BigDecimal("100.00"), TransferStatus.COMPLETED);
        transferWithId(5L, existing);
        existing.setChaveRetry("key-123");

        when(transferRepository.findByChaveRetry("key-123")).thenReturn(Optional.of(existing));

        TransferResponse response = transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.00")), "key-123");

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).findAllByIdForUpdate(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private Account accountWithId(Long id, Account account) {
        try {
            var idField = Account.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        return account;
    }

    private void transferWithId(Long id, Transfer transfer) {
        try {
            var idField = Transfer.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(transfer, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
