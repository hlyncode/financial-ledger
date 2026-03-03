package com.ledger.transfer.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaErrorHandler implements CommonErrorHandler {
    private final KafkaTemplate<String, String> kafkaTemplate;
    public KafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    @Override
    public boolean handleOne(Exception thrownException,
                             ConsumerRecord<?, ?> record,
                             Consumer<?, ?> consumer,
                             MessageListenerContainer container) {
        handle(thrownException, record);
        return true;
    }
    @Override
    public void handleOtherException(Exception thrownException,
                                     Consumer<?, ?> consumer,
                                     MessageListenerContainer container,
                                     boolean batchListener) {
        log.error("Erro no listener Kafka sem record associado. erro={}",
                thrownException.getMessage(), thrownException);
    }
    private void handle(Exception exception, ConsumerRecord<?, ?> record) {
        log.error("Mensagem enviada para DLQ. topic={}, partition={}, offset={}, erro={}",
                record.topic(), record.partition(), record.offset(), exception.getMessage());
        String dlqTopic = record.topic() + ".dlq";
        String payload = record.value() != null ? record.value().toString() : "null";
        kafkaTemplate.send(dlqTopic, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha crítica ao enviar para DLQ. dlqTopic={}", dlqTopic, ex);
                    } else {
                        log.info("Mensagem enviada para DLQ com sucesso. dlqTopic={}", dlqTopic);
                    }
                });
    }
}