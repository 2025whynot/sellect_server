package com.sellect.server.product.application;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public class FakeStorageService implements StorageService {

    private static final Path BASE_PATH = Paths.get("/fake/storage");

    @Override
    public void init() throws Exception {
        // 초기화 로직 없음
    }

    @Override
    public Path store(MultipartFile file) {
        // 가짜 저장 경로 반환 (파일명을 포함한 경로)
        return BASE_PATH.resolve(Objects.requireNonNull(file.getOriginalFilename()));
    }

    @Override
    public Stream<Path> loadAll() {
        return Stream.empty();
    }

    @Override
    public Path load(String filename) {
        return BASE_PATH.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        return null;
    }

    @Override
    public void deleteAll() {
        // 삭제 로직 없음
    }
}
