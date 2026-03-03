package com.ledger.transfer.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private static final int PARTITIONS = 3;  //em produção esse número aumenta conforme o volume de transações
    private static final int REPLICAS = 1;
    //comandos enviados pelo orquestrador para os serviços
    @Bean
    public NewTopic debitRequestedTopic() {
        return TopicBuilder.name("saga.debit.requested")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
    @Bean
    public NewTopic creditRequestedTopic() {
        return TopicBuilder.name("saga.credit.requested")
               .partitions(PARTITIONS)
               .replicas(REPLICAS)
               .build(); 
    }
    @Bean
    public NewTopic debitReversalRequestedTopic() {
        return TopicBuilder.name("saga.debit.reversal.requested")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
    //respostas enviadas pelos serviços de volta para o orquestrador
    @Bean
    public NewTopic debitPerformedTopic() {
        return TopicBuilder.name("saga.debit.performed")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
    @Bean
    public NewTopic debitFailedTopic () {
        return TopicBuilder.name("saga.debit.failed")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
    @Bean
    public NewTopic creditPerformedTopic() {
        return TopicBuilder.name("saga.credit.performed")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
    @Bean
    public NewTopic creditFailedTopic() {
        return TopicBuilder.name("saga.credit.failed")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
    @Bean 
    public NewTopic debitReversedTopic() {
        return TopicBuilder.name("saga.debit.reversed")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
    @Bean
    public NewTopic debitRequestedDlq() {
        return TopicBuilder.name("saga.debit.requested.dlq")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic creditRequestedDlq() {
        return TopicBuilder.name("saga.credit.requested.dlq")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic debitPerformedDlq() {
        return TopicBuilder.name("saga.debit.performed.dlq")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic creditPerformedDlq() {
        return TopicBuilder.name("saga.credit.performed.dlq")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic debitReversalDlq() {
        return TopicBuilder.name("saga.debit.reversal.requested.dlq")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}