package com.ledger.reserve.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.events.SagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveSagaConsumer {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    @KafkaListener(topics = "saga.credit.requested", groupId = "reserve-service")
    @Transactional
    public void onCreditRequested(String message, Acknowledgment ack) {
        try {
            SagaEvent event = objectMapper.readValue(message, SagaEvent.class);
            log.info("Crédito solicitado. sagaId={}, accountId={}, amount={}",
                    event.sagaId(), event.toAccountId(), event.amount());
            boolean creditSuccess = true;
            SagaEvent reply;
            String topic;
            if (creditSuccess) {
                reply = SagaEvent.of(
                        event.sagaId(), "CREDIT_PERFORMED",
                        event.fromAccountId(), event.toAccountId(),
                        event.amount(), event.currency()
                );
                topic = "saga.credit.performed";
            } else {
                reply = SagaEvent.failure(
                        event.sagaId(), "CREDIT_FAILED", "Conta destino bloqueada"
                );
                topic = "saga.credit.failed";
            }
            OutboxEvent outbox = OutboxEvent.create(
                    event.sagaId(), reply.eventType(),
                    topic, event.sagaId().toString(),
                    objectMapper.writeValueAsString(reply)
            );
            outboxEventRepository.save(outbox);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Erro ao processar CREDIT_REQUESTED. erro={}", e.getMessage(), e);
        }
    }
} 