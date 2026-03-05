package com.ledger.reserve.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Reserve {

    private final UUID id;
    private final UUID sagaId;
    private final UUID targetAccountId;
    private final BigDecimal amount;
    private final String currency;
    private ReserveStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Reserve(UUID id, UUID sagaId, UUID targetAccountId, BigDecimal amount, String currency) {
        this.id = id;
        this.sagaId = sagaId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.currency = currency;
        this.status = ReserveStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public ReserveStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void confirm() {
        this.status = ReserveStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = ReserveStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public boolean isPending() {
        return status == ReserveStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == ReserveStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == ReserveStatus.CANCELLED;
    }

    public enum ReserveStatus {
        PENDING,
        CONFIRMED,
        CANCELLED
    }
}
