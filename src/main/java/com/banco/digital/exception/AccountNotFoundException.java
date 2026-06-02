package com.banco.digital.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long accountId) {
        super("Conta não encontrada: " + accountId);
    }
}
