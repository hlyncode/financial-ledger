package com.ledger.transfer.saga;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_executions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaExecution {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "saga_type", nullable = false)
    private String sagaType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count")
    private int retryCount;

    public static SagaExecution create(String idempotencyKey, String payload) {
        SagaExecution saga = new SagaExecution();
        saga.id = UUID.randomUUID();
        saga.sagaType = "TRANSFER_SAGA";
        saga.status = SagaStatus.STARTED;
        saga.payload = payload;
        saga.idempotencyKey = idempotencyKey;
        saga.createdAt = OffsetDateTime.now();
        saga.updatedAt = OffsetDateTime.now();
        saga.retryCount = 0;
        return saga;
    }
    public void transitionTo(SagaStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = OffsetDateTime.now();
        if (newStatus == SagaStatus.COMPLETED || 
            newStatus == SagaStatus.COMPENSATED ||
            newStatus == SagaStatus.FAILED) {
            this.completedAt = OffsetDateTime.now();
        }
    }
    public void fail(String reason) {
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = OffsetDateTime.now();
        this.completedAt = OffsetDateTime.now();
    }
    public void incrementRetry() {
        this.retryCount++;
        this.updatedAt = OffsetDateTime.now();
    }
    public boolean isTerminal() {
        return status == SagaStatus.COMPLETED ||
               status == SagaStatus.COMPENSATED ||
               status == SagaStatus.FAILED;
    }
} 