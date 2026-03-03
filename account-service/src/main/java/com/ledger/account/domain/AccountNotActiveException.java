package com.ledger.account.domain;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(String message) {
        super (message);
    }
}