package com.sellect.server.common.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void produce(String topic, T message) {
        kafkaTemplate.send(topic, message);
        log.debug("Produced message to topic {}: {}", topic, message);
    }

    public <T> void produce(String topic, String key, T message) {
        kafkaTemplate.send(topic, key, message);
        log.debug("Produced message with key {} to topic {}: {}", key, topic, message);
    }
}
