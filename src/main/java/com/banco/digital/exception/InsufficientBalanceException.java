package com.banco.digital.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long accountId, BigDecimal balance, BigDecimal amount) {
        super("Saldo insuficiente na conta " + accountId
                + " (tem " + balance + ", pediu " + amount + ")");
    }
}
