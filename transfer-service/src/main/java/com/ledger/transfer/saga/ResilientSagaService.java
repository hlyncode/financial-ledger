package com.ledger.transfer.saga;

import com.ledger.common.events.SagaEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientSagaService {
    private final SagaOrchestrator sagaOrchestrator;
    @Retry(name = "sagaOrchestrator", fallbackMethod = "startFallback")
    @CircuitBreaker(name = "sagaOrchestrator", fallbackMethod = "startFallback")
    public SagaExecution start(String idempotencyKey, UUID fromAccountId,
                                UUID toAccountId, BigDecimal amount, String currency) {
        return sagaOrchestrator.start(idempotencyKey, fromAccountId,
                toAccountId, amount, currency);
    }
    @Retry(name = "sagaOrchestrator")
    @CircuitBreaker(name = "sagaOrchestrator")
    public void onDebitPerformed(SagaEvent event) {
        sagaOrchestrator.onDebitPerformed(event);
    }
    @Retry(name = "sagaOrchestrator")
    @CircuitBreaker(name = "sagaOrchestrator")
    public void onDebitFailed(SagaEvent event) {
        sagaOrchestrator.onDebitFailed(event);
    }
    @Retry(name = "sagaOrchestrator")
    @CircuitBreaker(name = "sagaOrchestrator")
    public void onCreditPerformed(SagaEvent event) {
        sagaOrchestrator.onCreditPerformed(event);
    }
    @Retry(name = "sagaOrchestrator")
    @CircuitBreaker(name = "sagaOrchestrator")
    public void onCreditFailed(SagaEvent event) {
        sagaOrchestrator.onCreditFailed(event);
    }
    @Retry(name = "sagaOrchestrator")
    @CircuitBreaker(name = "sagaOrchestrator")
    public void onDebitReversed(SagaEvent event) {
        sagaOrchestrator.onDebitReversed(event);
    }
    //fallback: chamado quando todas as tentativas falharam
    public SagaExecution startFallback(String idempotencyKey, UUID fromAccountId,
                                        UUID toAccountId, BigDecimal amount,
                                        String currency, Exception e) {
        log.error("Todas as tentativas falharam ao iniciar Saga. " +
                  "idempotencyKey={}, erro={}", idempotencyKey, e.getMessage());
        throw new SagaUnavailableException(
                "Serviço temporariamente indisponível. Tente novamente em instantes."
        );
    }
}