package com.ledger.transfer.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${ledger.outbox.batch-size:50}")
    private int batchSize;
    @Scheduled(fixedDelayString = "${ledger.outbox.polling-interval-ms:100}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(batchSize);

        if (pendingEvents.isEmpty()) {
            return;
        }
        log.debug("OutboxWorker encontrou {} eventos pendentes.", pendingEvents.size());
        for (OutboxEvent event : pendingEvents) {
            try {
                //publica no tópico kafka usando a partitionKey para garantir
                //que eventos do mesmo saga vão para a mesma partição — preservando a ordem
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Falha ao publicar evento no kafka. eventId={}, topic={}, erro={}",
                                        event.getId(), event.getTopic(), ex.getMessage());
                            }
                        });
                event.markAsPublished();
                event.incrementAttempts();
                outboxEventRepository.save(event);
                log.debug("Evento publicado com sucesso. eventId={}, topic={}, type={}",
                        event.getId(), event.getTopic(), event.getEventType());
            } catch (Exception e) {
                event.incrementAttempts();
                outboxEventRepository.save(event);
                log.error("Erro ao processar evento do Outbox. eventId={}, attempts={}, erro={}",
                        event.getId(), event.getAttempts(), e.getMessage());
            }
        }
    }
}