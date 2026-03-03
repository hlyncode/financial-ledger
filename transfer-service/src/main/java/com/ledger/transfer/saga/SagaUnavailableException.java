package com.ledger.transfer.saga;

public class SagaUnavailableException extends RuntimeException {
    public SagaUnavailableException(String message) {
        super(message);
    }
}