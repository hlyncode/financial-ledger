package com.ledger.transfer.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.events.SagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaReplyConsumer {
    private final SagaOrchestrator sagaOrchestrator;
    private final ObjectMapper objectMapper;
    @KafkaListener(topics = "saga.debit.performed", groupId = "transfer-service")
    public void onDebitPerformed(String message, Acknowledgment ack) {
        handle(message, ack, "DEBIT_PERFORMED", sagaOrchestrator::onDebitPerformed);
    }
    @KafkaListener(topics = "saga.debit.failed", groupId = "transfer-service")
    public void onDebitFailed(String message, Acknowledgment ack) {
        handle(message, ack, "DEBIT_FAILED", sagaOrchestrator::onDebitFailed);
    }
    @KafkaListener(topics = "saga.credit.performed", groupId = "transfer-service")
    public void onCreditPerformed(String message, Acknowledgment ack) {
        handle(message, ack, "CREDIT_PERFORMED", sagaOrchestrator::onCreditPerformed);
    }
    @KafkaListener(topics = "saga.credit.failed", groupId = "transfer-service")
    public void onCreditFailed(String message, Acknowledgment ack) {
        handle(message, ack, "CREDIT_FAILED", sagaOrchestrator::onCreditFailed);
    }
    @KafkaListener(topics = "saga.debit.reversed", groupId = "transfer-service")
    public void onDebitReversed(String message, Acknowledgment ack) {
        handle(message, ack, "DEBIT_REVERSED", sagaOrchestrator::onDebitReversed);
    }

    private void handle(String message, Acknowledgment ack,
                        String eventType, java.util.function.Consumer<SagaEvent> handler) {
        try {
            SagaEvent event = objectMapper.readValue(message, SagaEvent.class);
            log.info("Evento recebido. type={}, sagaId={}", eventType, event.sagaId());
            handler.accept(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Erro ao processar evento {}. erro={}", eventType, e.getMessage(), e);
        }
    }
} 