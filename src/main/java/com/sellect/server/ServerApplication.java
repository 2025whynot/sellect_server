package com.sellect.server;

import com.sellect.server.search.batch.TestDataGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

//     배치 성능 테스트 데이터 생성 용도
//    @Bean
//    public CommandLineRunner run(TestDataGenerator generator) {
//        return args -> {
//            System.out.println("Generating test data...");
//            generator.generateSearchLogData();
//            System.out.println("Test data generation completed.");
//        };
//    }

}
