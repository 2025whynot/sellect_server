package com.sellect.server.product.application;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    void init() throws Exception;

    String storeAndReturnNewFilename(MultipartFile file);

    String loadAsPath(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();

}
