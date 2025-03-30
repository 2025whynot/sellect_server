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

    public CompletableFuture<SendResult<String, Object>> produce(String topic, Object message) {
        log.debug("Producing message to topic {}: {}", topic, message);
        return kafkaTemplate.send(topic, message)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message to topic {}: {}", topic, message, ex);
                    sendToDlq(topic, message, ex); // 실패 시 DLQ로 전송
                } else {
                    log.debug("Successfully sent message to topic {}: offset={}",
                        topic, result.getRecordMetadata().offset());
                }
            });
    }

    public CompletableFuture<SendResult<String, Object>> produce(String topic, String key, Object message) {
        log.debug("Producing message with key {} to topic {}: {}", key, topic, message);
        return kafkaTemplate.send(topic, key, message)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send message with key {} to topic {}: {}", key, topic, message, ex);
                    sendToDlq(topic, message, ex); // 실패 시 DLQ로 전송
                } else {
                    log.debug("Successfully sent message with key {} to topic {}: offset={}",
                        key, topic, result.getRecordMetadata().offset());
                }
            });
    }

    public CompletableFuture<Object> produceWithReply(String requestTopic, String replyTopic, Object message) {
        log.debug("Producing message with reply to topic {}: {}", requestTopic, message);
        ProducerRecord<String, Object> record = new ProducerRecord<>(requestTopic, message);
        record.headers().add("replyTopic", replyTopic.getBytes()); // 응답 토픽 지정
        RequestReplyFuture<String, Object, Object> future = replyingKafkaTemplate.sendAndReceive(record);
        return future.exceptionally(throwable -> {
                log.error("Failed to get reply from topic {}: {}", requestTopic, message, throwable);
                sendToDlq(requestTopic, message, throwable); // 응답 실패 시 DLQ로 전송
                return null; // 기본 응답값 반환
            })
            .thenApply(ConsumerRecord::value); // 응답 값만 추출
    }

    private void sendToDlq(String originalTopic, Object message, Throwable ex) {
        String dlqTopic = originalTopic + "-dlq";
        log.warn("Sending failed message to DLQ topic {}: {}", dlqTopic, message);
        kafkaTemplate.send(dlqTopic, message)
            .whenComplete((result, dlqEx) -> {
                if (dlqEx != null) {
                    log.error("Failed to send to DLQ topic {}: {}", dlqTopic, message, dlqEx);
                } else {
                    log.debug("Successfully sent to DLQ topic {}: offset={}",
                        dlqTopic, result.getRecordMetadata().offset());
                }
            });
    }
}