package com.basilcode.emsbackend.common.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UnAuthorizeException extends RuntimeException {
    public UnAuthorizeException(String message) {
        super(message);
    }
}

