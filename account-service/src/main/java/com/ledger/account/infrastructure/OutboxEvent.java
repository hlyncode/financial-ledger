package com.ledger.account.infrastructure;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;
    
    @Column(name = "sagaId", nullable = false, columnDefinition = "uuid")
    private UUID sagaId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key")
    private String partitionKey;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private int attempts;

    public static OutboxEvent create(UUID sagaId, String eventType,
                                     String topic, String partitionKey,
                                     String payload) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.sagaId = sagaId;
        event.eventType = eventType;
        event.topic = topic;
        event.partitionKey = partitionKey;
        event.payload = payload;
        event.published = false;
        event.createdAt = OffsetDateTime.now();
        event.attempts = 0;
        return event;
    }
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = OffsetDateTime.now();
    }
    public void incrementAttempts() {
        this.attempts++;
    }
}