package com.sellect.server.common.kafka;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReplyingKafkaTemplate<String, Object, Object> replyingKafkaTemplate;

    public CompletableFuture<SendResult<String, Object>>  produce(String topic, Object message) {
        log.debug("Produced message to topic {}: {}", topic, message);
        return kafkaTemplate.send(topic, message);
    }

    public CompletableFuture<SendResult<String, Object>> produce(String topic, String key, Object message) {
        log.debug("Produced message with key {} to topic {}: {}", key, topic, message);
        return kafkaTemplate.send(topic, key, message);
    }

    public CompletableFuture<Object> produceWithReply(String requestTopic, String replyTopic, Object message) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(requestTopic, message);
        record.headers().add("replyTopic", replyTopic.getBytes()); // 응답 토픽 지정
        RequestReplyFuture<String, Object, Object> future = replyingKafkaTemplate.sendAndReceive(record);
        return future.thenApply(ConsumerRecord::value); // 응답 값만 추출
    }
}
