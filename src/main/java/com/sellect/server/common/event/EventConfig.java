package com.sellect.server.common.event;

import com.sellect.server.common.kafka.KafkaEventPublisher;
import com.sellect.server.common.kafka.KafkaProducer;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventConfig {

    @Bean
    public TransactionalEventPublisher transactionalEventPublisher(
        Map<String, EventPublisher> publishers) {
        return new TransactionalEventPublisher(publishers);
    }

    @Bean
    public EventPublisher kafkaEventPublisher(KafkaProducer kafkaProducer) {
        return new KafkaEventPublisher(kafkaProducer);
    }

//    @Bean
//    public EventPublisher springEventPublisher(ApplicationEventPublisher publisher) {
//        return new SpringEventPublisher(publisher);
//    }
}