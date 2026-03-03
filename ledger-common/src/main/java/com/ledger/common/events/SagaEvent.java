package com.ledger.common.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SagaEvent(
    UUID sagaId,
    String eventType,
    UUID fromAccountId,
    UUID toAccountId,
    BigDecimal amount,
    String currency,
    String status,
    String failureReason,
    OffsetDateTime occurredAt
) {
    public static SagaEvent of(UUID sagaId, String eventType,
                                UUID fromAccountId, UUID toAccountId,
                                BigDecimal amount, String currency) {
        return new SagaEvent(
            sagaId, eventType,
            fromAccountId, toAccountId,
            amount, currency,
            null, null,
            OffsetDateTime.now()
        );
    }
    public static SagaEvent failure(UUID sagaId, String eventType, String reason) {
        return new SagaEvent(
            sagaId, eventType,
            null, null,
            null, null,
            "FAILED", reason,
            OffsetDateTime.now()
        );
    }
}