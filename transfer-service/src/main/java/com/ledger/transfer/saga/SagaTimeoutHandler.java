package com.ledger.transfer.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutHandler {

    private final SagaExecutionRepository sagaExecutionRepository;
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void handleTimedOutSagas() {
        OffsetDateTime timeout = OffsetDateTime.now().minusMinutes(30);
        List<SagaExecution> timedOut = sagaExecutionRepository
                .findTimedOutSagas(timeout);
        if (timedOut.isEmpty()) return;
        log.warn("SagaTimeoutHandler encontrou {} sagas expiradas.", timedOut.size());
        for (SagaExecution saga : timedOut) {
            saga.fail("Saga expirou por timeout após 30 minutos. " +
                      "Último status: " + saga.getStatus());
            sagaExecutionRepository.save(saga);
            log.error("Saga marcada como FAILED por timeout. sagaId={}, status={}",
                    saga.getId(), saga.getStatus());
        }
    }
}