package com.ledger.transfer.saga;

public enum SagaStatus {
    STARTED,
    DEBIT_REQUESTED,
    DEBIT_PERFORMED,
    CREDIT_REQUESTED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}