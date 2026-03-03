package com.ledger.account.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    @Scheduled(fixedDelay = 100)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(50);
        if (pendingEvents.isEmpty()) return;
        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload());
                event.markAsPublished();
                event.incrementAttempts();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                event.incrementAttempts();
                outboxEventRepository.save(event);
                log.error("Erro ao publicar evento. eventId={}, erro={}", event.getId(), e.getMessage());
            }
        }
    }
}