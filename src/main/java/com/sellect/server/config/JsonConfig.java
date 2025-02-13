package com.sellect.server.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(
            new Jdk8Module()); // jdk 8 버전 이후 클래스들을(ex. Optional 등...) 파싱하거나 serialize, deserialize 하기 위해
        objectMapper.registerModule(new JavaTimeModule()); // localDate 등...

        // java.sql.Time 커스텀 설정
        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(Time.class, new JsonSerializer<>() {
            @Override
            public void serialize(Time value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
                gen.writeString(value.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
        });
        timeModule.addDeserializer(Time.class, new JsonDeserializer<>() {
            @Override
            public Time deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
                String timeString = p.getText();
                LocalTime localTime = LocalTime.parse(timeString,
                    DateTimeFormatter.ofPattern("HH:mm"));
                return Time.valueOf(localTime);
            }
        });

        // 커스텀한 java.sql.Time objectMapper에 적용
        objectMapper.registerModule(timeModule);

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false); // 모르는 json field에 대해서는 무시한다는 뜻 , 즉, 쓸모없는 데이터도 같이 올 경우 에러를 발생할건지, 아니면 무시할건지를 정하는 옵션

        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 날짜 관련 직렬화
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 스네이크 케이스
        objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());

        return objectMapper;
    }
}
