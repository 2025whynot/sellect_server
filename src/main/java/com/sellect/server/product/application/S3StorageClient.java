package com.sellect.server.product.application;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sellect.server.common.exception.CommonException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.config.properties.S3StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Primary
public class S3StorageClient implements StorageClient {

    private final AmazonS3 s3Client;
    private final String bucketName;

    @Autowired
    public S3StorageClient(AmazonS3 s3Client, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucketName = properties.getBucketName();
    }

    @Override
    @PostConstruct
    public void init() throws Exception {
        // 현재 S3 사용하지 않는 관계로 주석 처리
//        if (!s3Client.doesBucketExistV2(bucketName)) {
//            throw new Exception("bucket does not exist");
//        }
    }

    @Override
    public void store(MultipartFile file, String filename) {
        try (InputStream inputStream = file.getInputStream()) {
            validateFilename(filename);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, filename, inputStream, metadata));
        } catch (IOException e) {
            throw new CommonException(BError.FAIL_FOR_REASON, "store file", e.getMessage());
        }
    }

    @Override
    public void store(InputStream inputStream, String filename) {
        try {
            validateFilename(filename);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(inputStream.available());

            s3Client.putObject(new PutObjectRequest(bucketName, filename, inputStream, metadata));
        } catch (IOException e) {
            throw new CommonException(BError.FAIL_FOR_REASON, "store file", e.getMessage());
        }
    }

    @Override
    public String loadAsPath(String filename) {
        if (!s3Client.doesObjectExist(bucketName, filename)) {
            throw new CommonException(BError.NOT_EXIST, "file");
        }
        return s3Client.getUrl(bucketName, filename).toString();
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            URI fileUri = URI.create(loadAsPath(filename));
            Resource resource = new UrlResource(fileUri);
            if (!resource.exists() || !resource.isReadable()) {
                throw new CommonException(BError.NOT_EXIST, "file");
            }
            return resource;
        } catch (Exception e) {
            throw new CommonException(BError.FAIL_FOR_REASON, "load file", e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static void validateFilename(String filename) {
        if (Objects.isNull(filename) || filename.isBlank()) {
            throw new CommonException(BError.NOT_EXIST, "file name");
        }
    }
}
