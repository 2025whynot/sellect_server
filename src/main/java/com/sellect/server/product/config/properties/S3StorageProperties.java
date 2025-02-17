package com.sellect.server.product.config.properties;

import com.amazonaws.regions.Regions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("storage.s3")
public class S3StorageProperties {

    private String bucketName = "";
    private String accessKey = "";
    private String secretKey = "";
    private String region = Regions.AP_NORTHEAST_2.getName();

}
