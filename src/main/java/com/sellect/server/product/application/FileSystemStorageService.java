package com.sellect.server.product.application;

import com.sellect.server.common.exception.StorageException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.exception.enums.IError;
import com.sellect.server.product.properties.StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {

        if(properties.getLocation().trim().isEmpty()){
            throw new StorageException(BError.FAIL_FOR_REASON,
                "FileSystemStorageService initialization",
                "upload location is empty");
        }
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public Path store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException(BError.NOT_EXIST, "file");
            }

            String newFilename = generateNewFilename(Objects.requireNonNull(file.getOriginalFilename()));
            Path destinationFile = this.rootLocation.resolve(
                    Paths.get(newFilename))
                .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException(BError.FAIL_FOR_REASON,
                    "store file",
                    "cannot store file outside current directory.");
            }

            // InputStream 자원을 사용한 후에는 반드시 닫아주어야 한다
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                    StandardCopyOption.REPLACE_EXISTING);
            }

            return destinationFile;
        }
        catch (IOException e) {
            throw new StorageException(BError.FAIL_FOR_REASON,
                "store file",
                e.getMessage());
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                .filter(path -> !path.equals(this.rootLocation))
                .map(this.rootLocation::relativize);
        }
        catch (IOException e) {
            throw new StorageException(BError.FAIL_FOR_REASON,
                "load files",
                e.getMessage());
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException(IError.RESOURCE_NOT_ALIVE);
            }
        }
        catch (MalformedURLException e) {
            throw new StorageException(IError.RESOURCE_NOT_ALIVE);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException(BError.FAIL_FOR_REASON,
                "init storage",
                e.getMessage());
        }
    }

    private String generateNewFilename(String originalFilename) {
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String baseName = UUID.randomUUID().toString();
        return baseName + "_" + System.currentTimeMillis() + fileExtension;
    }
}
