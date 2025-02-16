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
import java.util.UUID;
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
    public String storeAndReturnNewFilename(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String originalFilename = file.getOriginalFilename();
            if (Objects.isNull(originalFilename) || originalFilename.isBlank()) {
                throw new StorageException(BError.NOT_EXIST, "file name");
            }
            String newFilename = generateNewFilename(originalFilename);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, newFilename, inputStream, metadata));

            return newFilename;
        } catch (IOException e) {
            throw new StorageException(BError.FAIL_FOR_REASON, "store file", e.getMessage());
        }
    }

    @Override
    public String loadAsPath(String filename) {
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

    private String generateNewFilename(String originalFilename) {
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String baseName = UUID.randomUUID().toString();
        return baseName + "_" + System.currentTimeMillis() + fileExtension;
    }
}
