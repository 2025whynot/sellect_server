package com.sellect.server.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final String BASE_PACKAGES = "com.sellect.server.*";
    private static final String PAY_READY_GROUP = "pay-ready-group";
    private static final String PAY_APPROVE_GROUP = "pay-approve-group";
    private static final String PAY_APPROVE_DLQ_GROUP = "pay-approve-dlq-group";
    private static final String ORDER_COMPLETE_GROUP = "order-complete-group";
    private static final String ORDER_COMPLETE_ROLLBACK_GROUP = "order-complete-rollback-group";
    private static final String ORDER_COMPLETE_REPLY_GROUP = "order-complete-reply-group";
    private static final String ORDER_COMPLETE_DLQ_GROUP = "order-complete-dlq-group";
    private static final Integer BACK_OFF_INTERVAL = 0;
    private static final Integer MAX_ATTEMPTS = 0;

    @Value("${spring.kafka.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS;
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String AUTO_OFFSET_RESET;

    // Producer 설정
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.RETRIES_CONFIG, MAX_ATTEMPTS); // 재시도 횟수
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, BACK_OFF_INTERVAL); // 재시도 간격
        return new DefaultKafkaProducerFactory<>(props);
    }

    // KafkaTemplate 설정
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // (order-complete-reply) ReplyingKafkaTemplate 설정
    @Bean
    public ReplyingKafkaTemplate<String, Object, Object> orderCompleteReplyingKafkaTemplate() {
        ConcurrentMessageListenerContainer<String, Object> container = orderCompleteReplyContainerFactory()
            .createContainer("order-complete-reply");
        container.getContainerProperties().setGroupId(ORDER_COMPLETE_REPLY_GROUP);
        return new ReplyingKafkaTemplate<>(producerFactory(), container);
    }

    // 공통 ConsumerFactory 생성 메서드
    private ConsumerFactory<String, Object> consumerFactory(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, BASE_PACKAGES);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 공통 ConcurrentKafkaListenerContainerFactory 생성 메서드
    private ConcurrentKafkaListenerContainerFactory<String, Object> listenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory,
        KafkaTemplate<String, Object> replyTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        if (replyTemplate != null) {
            factory.setReplyTemplate(replyTemplate); // 응답이 필요한 경우에만 설정
        }

        // 에러 핸들러 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate(),
            (record, ex) ->
                new TopicPartition(record.topic() + "-dlq", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
            new FixedBackOff(BACK_OFF_INTERVAL, MAX_ATTEMPTS));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // (pay-ready-group) Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> payReadyGroupConsumer() {
        return consumerFactory(PAY_READY_GROUP);
    }

    // (pay-ready-group) Listener 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        return listenerContainerFactory(payReadyGroupConsumer(), null);
    }

    // (pay-approve-group) Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> payApproveGroupConsumer() {
        return consumerFactory(PAY_APPROVE_GROUP);
    }

    // (pay-approve-group) Listener 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> payApproveContainerFactory() {
        return listenerContainerFactory(payApproveGroupConsumer(), null);
    }

    // (order-complete-group) Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> orderCompleteGroupConsumer() {
        return consumerFactory(ORDER_COMPLETE_GROUP);
    }

    // (order-complete-group) Listener 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderCompleteContainerFactory() {
        // 응답(order-complete-reply)이 필요하므로 KafkaTemplate을 설정
        return listenerContainerFactory(orderCompleteGroupConsumer(), kafkaTemplate());
    }

    // (order-complete-rollback-group) Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> orderCompleteRollbackGroupConsumer() {
        return consumerFactory(ORDER_COMPLETE_ROLLBACK_GROUP);
    }

    // (order-complete-rollback-group) Listener 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderCompleteRollbackContainerFactory() {
        return listenerContainerFactory(orderCompleteRollbackGroupConsumer(), null);
    }

    // (order-complete-reply-group) Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> orderCompleteReplyGroupConsumer() {
        return consumerFactory(ORDER_COMPLETE_REPLY_GROUP);
    }

    // (order-complete-reply-group) Listener 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderCompleteReplyContainerFactory() {
        return listenerContainerFactory(orderCompleteReplyGroupConsumer(), null);
    }

    // === DLQ용 추가 설정 === //

    // (order-complete-dlq-group) Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> orderCompleteDlqGroupConsumer() {
        return consumerFactory(ORDER_COMPLETE_DLQ_GROUP);
    }

    // (order-complete-dlq-group) Listener 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> orderCompleteDlqContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCompleteDlqGroupConsumer());
        return factory;
    }

    // (pay-approve-dlq-group) Consumer 설정
    @Bean
    public ConsumerFactory<String, Object> payApproveDlqContainerFactory() {
        return consumerFactory(PAY_APPROVE_DLQ_GROUP);
    }

    // (pay-approve-dlq-group) Listener 설정
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> payApproveDlqGroupConsumer() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(payApproveDlqContainerFactory());
        return factory;
    }
}