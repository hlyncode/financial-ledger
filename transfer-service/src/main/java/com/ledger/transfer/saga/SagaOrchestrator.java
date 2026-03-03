package com.ledger.transfer.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.events.SagaEvent;
import com.ledger.transfer.infrastructure.SagaMetrics;
import com.ledger.transfer.outbox.OutboxEvent;
import com.ledger.transfer.outbox.OutboxEventRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {
    private final SagaExecutionRepository sagaExecutionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final SagaMetrics sagaMetrics;

    @Observed(name = "saga.start", contextualName = "iniciar-saga")
    @Transactional
    public SagaExecution start(String idempotencyKey,
                                UUID fromAccountId,
                                UUID toAccountId,
                                BigDecimal amount,
                                String currency) {
        try {
            SagaEvent initialEvent = SagaEvent.of(
                    null, "TRANSFER_REQUESTED",
                    fromAccountId, toAccountId,
                    amount, currency
            );
            String payload = objectMapper.writeValueAsString(initialEvent);
            SagaExecution saga = SagaExecution.create(idempotencyKey, payload);
            sagaExecutionRepository.save(saga);

            //cria o 1° evento no outbox — débito na conta origem
            //tudo na mesma transação: se um falhar, os dois revertem
            SagaEvent debitEvent = SagaEvent.of(
                    saga.getId(), "DEBIT_REQUESTED",
                    fromAccountId, toAccountId,
                    amount, currency
            );
            OutboxEvent outbox = OutboxEvent.create(
                    saga.getId(),
                    "DEBIT_REQUESTED",
                    "saga.debit.requested",
                    saga.getId().toString(), // partitionKey — garante ordem
                    objectMapper.writeValueAsString(debitEvent)
            );
            outboxEventRepository.save(outbox);
            saga.transitionTo(SagaStatus.DEBIT_REQUESTED);
            sagaExecutionRepository.save(saga);

            sagaMetrics.incrementSagasIniciadas();

            log.info("Saga iniciada. sagaId={}, from={}, to={}, amount={}",
                    saga.getId(), fromAccountId, toAccountId, amount);

            return saga;

        } catch (Exception e) {
            log.error("Erro ao iniciar Saga. idempotencyKey={}", idempotencyKey, e);
            throw new RuntimeException("Falha ao iniciar transferência", e);
        }
    }
    //débito realizado com sucesso — solicita crédito
    @Observed(name = "saga.debit.performed", contextualName = "debito-confirmado")
    @Transactional
    public void onDebitPerformed(SagaEvent event) {
        SagaExecution saga = findSaga(event.sagaId());
        if (saga.isTerminal()) {
            log.warn("Saga já está em estado terminal. sagaId={}", saga.getId());
            return;
        }
        try {
            saga.transitionTo(SagaStatus.DEBIT_PERFORMED);

            //próximo passo: solicita o crédito na conta de destino
            SagaEvent creditEvent = SagaEvent.of(
                    saga.getId(), "CREDIT_REQUESTED",
                    event.fromAccountId(), event.toAccountId(),
                    event.amount(), event.currency()
            );
            OutboxEvent outbox = OutboxEvent.create(
                    saga.getId(),
                    "CREDIT_REQUESTED",
                    "saga.credit.requested",
                    saga.getId().toString(),
                    objectMapper.writeValueAsString(creditEvent)
            );
            outboxEventRepository.save(outbox);
            saga.transitionTo(SagaStatus.CREDIT_REQUESTED);
            sagaExecutionRepository.save(saga);

            sagaMetrics.incrementDebitosRealizados();

            log.info("Débito confirmado, solicitando crédito. sagaId={}", saga.getId());

        } catch (Exception e) {
            log.error("Erro ao processar DEBIT_PERFORMED. sagaId={}", saga.getId(), e);
            throw new RuntimeException(e);
        }
    }

    //débito falhou — saga encerra como FAILED (nada a compensar ainda)
    @Transactional
    public void onDebitFailed(SagaEvent event) {
        SagaExecution saga = findSaga(event.sagaId());
        if (saga.isTerminal()) return;
        saga.fail("Débito falhou: " + event.failureReason());
        sagaExecutionRepository.save(saga);

        sagaMetrics.incrementSagasFalhas();

        log.warn("Saga falhou no débito. sagaId={}, motivo={}",
                saga.getId(), event.failureReason());
    }
    //crédito realizado com sucesso — saga completed
    @Observed(name = "saga.credit.performed", contextualName = "credito-confirmado")
    @Transactional
    public void onCreditPerformed(SagaEvent event) {
        SagaExecution saga = findSaga(event.sagaId());
        if (saga.isTerminal()) return;
        saga.transitionTo(SagaStatus.COMPLETED);
        sagaExecutionRepository.save(saga);

        sagaMetrics.incrementSagasCompletadas();
        sagaMetrics.incrementCreditosRealizados();
        sagaMetrics.recordSagaDuration(
                java.time.Duration.between(saga.getCreatedAt(), OffsetDateTime.now()).toMillis()
        );

        log.info("Saga concluída com sucesso. sagaId={}", saga.getId());
    }

    //crédito falhou — inicia compensação (estorna o débito)
    @Observed(name = "saga.credit.failed", contextualName = "credito-falhou")
    @Transactional
    public void onCreditFailed(SagaEvent event) {
        SagaExecution saga = findSaga(event.sagaId());
        if (saga.isTerminal()) return;
        try {
            saga.transitionTo(SagaStatus.COMPENSATING);
            //solicita estorno do débito que já foi feito
            SagaEvent reversalEvent = SagaEvent.of(
                    saga.getId(), "DEBIT_REVERSAL_REQUESTED",
                    event.fromAccountId(), event.toAccountId(),
                    event.amount(), event.currency()
            );
            OutboxEvent outbox = OutboxEvent.create(
                    saga.getId(),
                    "DEBIT_REVERSAL_REQUESTED",
                    "saga.debit.reversal.requested",
                    saga.getId().toString(),
                    objectMapper.writeValueAsString(reversalEvent)
            );
            outboxEventRepository.save(outbox);
            sagaExecutionRepository.save(saga);
            log.warn("Crédito falhou, iniciando compensação. sagaId={}, motivo={}",
                    saga.getId(), event.failureReason());

        } catch (Exception e) {
            log.error("Erro ao iniciar compensação. sagaId={}", saga.getId(), e);
            throw new RuntimeException(e);
        }
    }

    //estorno concluído — saga compensated
    @Observed(name = "saga.debit.reversed", contextualName = "debito-estornado")
    @Transactional
    public void onDebitReversed(SagaEvent event) {
        SagaExecution saga = findSaga(event.sagaId());
        if (saga.isTerminal()) return;
        saga.transitionTo(SagaStatus.COMPENSATED);
        sagaExecutionRepository.save(saga);

        sagaMetrics.incrementSagasCompensadas();
        sagaMetrics.incrementEstornosRealizados();

        log.info("Saga compensada com sucesso. sagaId={}", saga.getId());
    }

    //utilitário
    private SagaExecution findSaga(UUID sagaId) {
        return sagaExecutionRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga não encontrada: " + sagaId));
    }
}