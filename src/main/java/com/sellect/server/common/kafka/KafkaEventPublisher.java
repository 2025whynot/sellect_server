package com.sellect.server.common.kafka;

import com.sellect.server.common.event.EventPublisher;

public class KafkaEventPublisher implements EventPublisher {
    private final KafkaProducer kafkaProducer;

    public KafkaEventPublisher(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void publish(Object event) {
        if (event instanceof KafkaEventMessage kafkaEventMessage) {
            if (kafkaEventMessage.getPartitionKey() != null) {
                kafkaProducer.produce(kafkaEventMessage.getTopic(), kafkaEventMessage.getPartitionKey(), kafkaEventMessage.getPayload());
            } else {
                kafkaProducer.produce(kafkaEventMessage.getTopic(), kafkaEventMessage.getPayload());
            }
        } else {
            throw new IllegalArgumentException("Invalid event type: " + event.getClass().getName());
        }
    }
}
