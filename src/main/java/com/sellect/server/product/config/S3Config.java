package com.sellect.server.product.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.sellect.server.product.config.properties.S3StorageProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Bean
    public AmazonS3 amazonS3(S3StorageProperties properties) {
        return AmazonS3ClientBuilder.standard()
            .withRegion(properties.getRegion())
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey()))
            )
            .build();
    }
}
