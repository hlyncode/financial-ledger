package com.ledger.account.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.account.application.CreditAccountUseCase;
import com.ledger.account.application.DebitAccountUseCase;
import com.ledger.account.application.DebitResult;
import com.ledger.common.events.SagaEvent;
import com.ledger.common.domain.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSagaConsumer {
    private final DebitAccountUseCase debitAccountUseCase;
    private final CreditAccountUseCase creditAccountUseCase;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    //consome pedido de débito
    @KafkaListener(topics = "saga.debit.requested", groupId = "account-service")
    @Transactional
    public void onDebitRequested(String message, Acknowledgment ack) {
        try {
            SagaEvent event = objectMapper.readValue(message, SagaEvent.class);
            log.info("Débito solicitado. sagaId={}, accountId={}, amount={}",
                    event.sagaId(), event.fromAccountId(), event.amount());
            Money amount = Money.of(event.amount(), event.currency());
            DebitResult result = debitAccountUseCase.execute(
                    event.fromAccountId(), amount, event.sagaId()
            );

            //publica resultado de volta via outbox
            SagaEvent reply;
            String topic;
            if (result.success()) {
                reply = SagaEvent.of(
                        event.sagaId(), "DEBIT_PERFORMED",
                        event.fromAccountId(), event.toAccountId(),
                        event.amount(), event.currency()
                );
                topic = "saga.debit.performed";
            } else {
                reply = SagaEvent.failure(event.sagaId(), "DEBIT_FAILED", result.failureReason());
                topic = "saga.debit.failed";
            }
            OutboxEvent outbox = OutboxEvent.create(
                    event.sagaId(), reply.eventType(),
                    topic, event.sagaId().toString(),
                    objectMapper.writeValueAsString(reply)
            );
            outboxEventRepository.save(outbox);
            ack.acknowledge(); //commit manual do offset kafka
        } catch (Exception e) {
            log.error("Erro ao processar DEBIT_REQUESTED. erro={}", e.getMessage(), e);
            //não faz ack — kafka vai retentar a mensagem
        }
    }

    //consome pedido de estorno (compensação)
    @KafkaListener(topics = "saga.debit.reversal.requested", groupId = "account-service")
    @Transactional
    public void onDebitReversalRequested(String message, Acknowledgment ack) {
        try {
            SagaEvent event = objectMapper.readValue(message, SagaEvent.class);
            log.info("Estorno solicitado. sagaId={}, accountId={}, amount={}",
                    event.sagaId(), event.fromAccountId(), event.amount());
            //estorno = crédito na conta origem
            Money amount = Money.of(event.amount(), event.currency());
            creditAccountUseCase.execute(
                    event.fromAccountId(), amount, event.sagaId()
            );
            SagaEvent reply = SagaEvent.of(
                    event.sagaId(), "DEBIT_REVERSED",
                    event.fromAccountId(), event.toAccountId(),
                    event.amount(), event.currency()
            );
            OutboxEvent outbox = OutboxEvent.create(
                    event.sagaId(), "DEBIT_REVERSED",
                    "saga.debit.reversed", event.sagaId().toString(),
                    objectMapper.writeValueAsString(reply)
            );
            outboxEventRepository.save(outbox);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Erro ao processar DEBIT_REVERSAL_REQUESTED. erro={}", e.getMessage(), e);
        }
    }
}