package com.banco.digital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banco.digital.dto.AccountRequest;
import com.banco.digital.dto.AccountResponse;
import com.banco.digital.dto.MovementResponse;
import com.banco.digital.entity.Account;
import com.banco.digital.entity.Transfer;
import com.banco.digital.entity.TransferStatus;
import com.banco.digital.exception.AccountNotFoundException;
import com.banco.digital.repository.AccountRepository;
import com.banco.digital.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccount() {
        AccountRequest request = new AccountRequest("Nova Conta", new BigDecimal("1500.00"));
        Account saved = new Account("Nova Conta", new BigDecimal("1500.00"));

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        AccountResponse response = accountService.create(request);

        assertThat(response.name()).isEqualTo("Nova Conta");
        assertThat(response.balance()).isEqualByComparingTo("1500.00");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldFindAccountById() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.findById(1L);

        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.balance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Conta não encontrada");
    }

    @Test
    void shouldReturnMovementsForAccount() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        Transfer outgoing = new Transfer(1L, 2L, new BigDecimal("100.00"), TransferStatus.COMPLETED);
        Transfer incoming = new Transfer(3L, 1L, new BigDecimal("50.00"), TransferStatus.COMPLETED);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transferRepository.findMovementsByAccountId(1L)).thenReturn(List.of(outgoing, incoming));

        List<MovementResponse> movements = accountService.getMovements(1L);

        assertThat(movements).hasSize(2);
        assertThat(movements.get(0).type()).isEqualTo("DEBIT");
        assertThat(movements.get(1).type()).isEqualTo("CREDIT");
    }
}
