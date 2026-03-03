package com.ledger.transfer.saga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaExecutionRepository extends JpaRepository<SagaExecution, UUID> {

    Optional<SagaExecution> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT s FROM SagaExecution s
            WHERE s.status NOT IN (
                com.ledger.transfer.saga.SagaStatus.COMPLETED,
                com.ledger.transfer.saga.SagaStatus.COMPENSATED,
                com.ledger.transfer.saga.SagaStatus.FAILED
            )
            AND s.createdAt < :timeout
            """)
    List<SagaExecution> findTimedOutSagas(@Param("timeout") OffsetDateTime timeout);
}