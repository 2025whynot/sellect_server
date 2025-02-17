package com.sellect.server.product.application;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    void init() throws Exception;

    void store(MultipartFile file, String filename);

    String loadAsPath(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();

}
