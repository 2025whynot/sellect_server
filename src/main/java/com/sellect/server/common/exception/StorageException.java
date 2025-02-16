package com.sellect.server.common.exception;

import com.sellect.server.common.exception.enums.Error;

public class StorageException extends CommonException {

    public StorageException(String code, String message) {
        super(code, message);
    }

    public StorageException(String code, String message, Throwable err) {
        super(code, message, err);
    }

    public StorageException(Error error, String... args) {
        super(error, args);
    }

}
