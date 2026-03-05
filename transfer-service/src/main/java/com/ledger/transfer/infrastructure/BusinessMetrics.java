package com.ledger.transfer.infrastructure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
@Component
public class BusinessMetrics {

    private final Counter transfersInitiated;
    private final Counter transfersCompleted;
    private final Counter transfersFailed;
    private final Counter transfersCompensated;
    private final Timer transferDuration;

    public BusinessMetrics(MeterRegistry registry) {
        this.transfersInitiated = Counter.builder("transfers.initiated")
                .description("Total de transferências iniciadas")
                .register(registry);

        this.transfersCompleted = Counter.builder("transfers.completed")
                .description("Total de transferências concluídas com sucesso")
                .register(registry);

        this.transfersFailed = Counter.builder("transfers.failed")
                .description("Total de transferências falhadas")
                .register(registry);

        this.transfersCompensated = Counter.builder("transfers.compensated")
                .description("Total de transferências compensadas (rollback)")
                .register(registry);

        this.transferDuration = Timer.builder("transfers.duration")
                .description("Tempo de processamento das transferências")
                .register(registry);
    }

    public void recordTransferInitiated() {
        transfersInitiated.increment();
    }

    public void recordTransferCompleted() {
        transfersCompleted.increment();
    }

    public void recordTransferFailed() {
        transfersFailed.increment();
    }

    public void recordTransferCompensated() {
        transfersCompensated.increment();
    }

    public void recordTransferDuration(long durationMs) {
        transferDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
