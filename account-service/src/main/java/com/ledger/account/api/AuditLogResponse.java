package com.ledger.account.api;

import com.ledger.account.infrastructure.AuditLog;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        UUID sagaId,
        String eventType,
        UUID accountId,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String currency,
        OffsetDateTime occurredAt,
        boolean integrityValid
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getSagaId(),
                log.getEventType(),
                log.getAccountId(),
                log.getAmount(),
                log.getBalanceBefore(),
                log.getBalanceAfter(),
                log.getCurrency(),
                log.getOccurredAt(),
                log.isIntegrityValid()
        );
    }
}