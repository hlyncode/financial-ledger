package com.ledger.transfer.infrastructure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SagaMetrics {
    //contadores: quantas vezes cada evento aconteceu
    private final Counter sagasIniciadas;
    private final Counter sagasCompletadas;
    private final Counter sagasFalhas;
    private final Counter sagasCompensadas;
    private final Counter debitosRealizados;
    private final Counter creditosRealizados;
    private final Counter estornosRealizados;

    //timer: quanto tempo as sagas levam para completar
    private final Timer sagaDuration;
    public SagaMetrics(MeterRegistry registry) {
        this.sagasIniciadas = Counter.builder("saga.iniciadas")
                .description("Total de Sagas iniciadas")
                .register(registry);

        this.sagasCompletadas = Counter.builder("saga.completadas")
                .description("Total de Sagas completada com sucesso")
                .register(registry);

        this.sagasFalhas = Counter.builder("saga.falhas")
                .description("Total de Sagas que falharam")
                .register(registry);

        this.sagasCompensadas = Counter.builder("saga.compensadas")
                .description("Total de Sagas compensadas")
                .register(registry);

        this.debitosRealizados = Counter.builder("saga.debitos.realizados")
                .description("Total de débitos realizados")
                .register(registry);

        this.creditosRealizados = Counter.builder("saga.creditos.realizados")
                .description("Total de créditos realizados")
                .register(registry);

        this.estornosRealizados = Counter.builder("saga.estornos.realizados")
                .description("Total de estornos realizados")
                .register(registry);

        this.sagaDuration = Timer.builder("saga.duration")
                .description("Tempo de duração das Sagas")
                .register(registry);
    }

    public void incrementSagasIniciadas()    { sagasIniciadas.increment(); }
    public void incrementSagasCompletadas()  { sagasCompletadas.increment(); }
    public void incrementSagasFalhas()       { sagasFalhas.increment(); }
    public void incrementSagasCompensadas()  { sagasCompensadas.increment(); }
    public void incrementDebitosRealizados() { debitosRealizados.increment(); }
    public void incrementCreditosRealizados(){ creditosRealizados.increment(); }
    public void incrementEstornosRealizados(){ estornosRealizados.increment(); }

    public void recordSagaDuration(long milliseconds) {
        sagaDuration.record(milliseconds, TimeUnit.MILLISECONDS);
    }
}