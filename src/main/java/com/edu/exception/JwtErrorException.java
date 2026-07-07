package com.edu.exception;

import org.springframework.http.HttpStatus;

/**
 * JWT自定义异常
 */
public class JwtErrorException extends BaseException {
    public JwtErrorException() {
    }

    public JwtErrorException(HttpStatus status, String msg) {
        super(status, msg);
    }

    public JwtErrorException(HttpStatus status, String msg, Object data) {
        super(status, msg, data);
    }
}
