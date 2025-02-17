package com.sellect.server.product.application;

import com.sellect.server.common.exception.StorageException;
import com.sellect.server.common.exception.enums.BError;
import com.sellect.server.common.exception.enums.IError;
import com.sellect.server.product.config.properties.FileSystemStorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Qualifier("fileSystemStorageService")
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(FileSystemStorageProperties properties) {

        if(properties.getLocation().trim().isEmpty()){
            throw new StorageException(BError.FAIL_FOR_REASON,
                "FileSystemStorageService initialization",
                "upload location is empty");
        }
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    @PostConstruct
    public void init() throws Exception {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new Exception("Could not initialize storage location", e);
        }
    }

    @Override
    public void store(MultipartFile file, String filename) {
        try {
            if (file.isEmpty()) {
                throw new StorageException(BError.NOT_EXIST, "file");
            }

            if (Objects.isNull(filename)) {
                throw new StorageException(BError.NOT_EXIST, "file name");
            }
            Path destinationFile = this.rootLocation.resolve(
                    Paths.get(filename))
                .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException(BError.FAIL_FOR_REASON,
                    "store file",
                    "cannot store file outside current directory.");
            }

            // InputStream 자원을 사용한 후에는 반드시 닫아주어야 함
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                    StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException(BError.FAIL_FOR_REASON,
                "store file",
                e.getMessage());
        }
    }

    @Override
    public String loadAsPath(String filename) {
        // TODO: 해당 파일명의 파일이 존재하는지 검증
        return rootLocation.resolve(filename).toString();
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
            Resource resource = new UrlResource(file.toUri());
            if (resource.getFile().isDirectory()) {
                throw new StorageException(IError.RESOURCE_NOT_ALIVE);
            }
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException(IError.RESOURCE_NOT_ALIVE);
            }
        } catch (IOException e) {
            throw new StorageException(IError.RESOURCE_NOT_ALIVE);
        }
    }

    @Override
    public void deleteAll() {
        try (Stream<Path> files = Files.list(rootLocation)) {
            files.forEach(path -> FileSystemUtils.deleteRecursively(path.toFile()));
        } catch (IOException e) {
            throw new StorageException(BError.FAIL_FOR_REASON, "delete files", e.getMessage());
        }
    }

}
