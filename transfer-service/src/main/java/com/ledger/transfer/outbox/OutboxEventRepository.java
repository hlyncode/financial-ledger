package com.ledger.transfer.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    //busca os próximos eventos pendentes por data de criação
    //limit garante que o worker processe em lotes controlados
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published = flase
            ORDER BY created_at ASC
            LIMIT :batchSize
            """, nativeQuery = true)
    List<OutboxEvent> findPendingEvents(@Param("batchSize") int batchSize);
}