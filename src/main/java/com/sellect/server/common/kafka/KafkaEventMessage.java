package com.sellect.server.common.kafka;

public interface KafkaEventMessage {
    String getTopic();         // 발행할 토픽 이름
    String getPartitionKey();  // 파티션 키 (null 가능)
    Object getPayload();       // 실제 메시지 내용
}
