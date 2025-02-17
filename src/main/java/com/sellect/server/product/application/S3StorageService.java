package com.sellect.server.product.application;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sellect.server.common.exception.StorageException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.product.config.properties.S3StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
public class S3StorageService implements StorageService {

    private final AmazonS3 s3Client;
    private final String bucketName;

    @Autowired
    public S3StorageService(AmazonS3 s3Client, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucketName = properties.getBucketName();
    }

    @Override
    public void init() {
        // S3는 별도의 초기화가 필요 없음
    }

    @Override
    public void store(MultipartFile file, String filename) {
        try (InputStream inputStream = file.getInputStream()) {
            if (Objects.isNull(filename) || filename.isBlank()) {
                throw new StorageException(BError.NOT_EXIST, "file name");
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, filename, inputStream, metadata));
        } catch (IOException e) {
            throw new StorageException(BError.FAIL_FOR_REASON, "store file", e.getMessage());
        }
    }

    @Override
    public String loadAsPath(String filename) {
        // TODO: 해당 파일명의 파일이 존재하는지 검증
        return s3Client.getUrl(bucketName, filename).toString();
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            URI fileUri = URI.create(loadAsPath(filename));
            Resource resource = new UrlResource(fileUri);
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException(BError.NOT_EXIST, "file");
            }
        } catch (Exception e) {
            throw new StorageException(BError.FAIL_FOR_REASON, "load file", e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
