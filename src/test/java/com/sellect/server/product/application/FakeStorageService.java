package com.sellect.server.product.application;

import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public class FakeStorageService implements StorageService {

    private final Map<String, String> storage = new HashMap<>();

    @Override
    public void init() {}

    @Override
    public String storeAndReturnNewFilename(MultipartFile file) {
        String newFilename = file.getOriginalFilename() + "_fake";
        storage.put(newFilename, "http://fake-url.com/" + newFilename);
        return newFilename;
    }

    @Override
    public String loadAsPath(String filename) {
        return storage.getOrDefault(filename, null);
    }

    @Override
    public Resource loadAsResource(String filename) {
        return null; // 테스트에 필요하면 추가 구현 가능
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }
}

