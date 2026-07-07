package com.edu.exception;

import org.springframework.http.HttpStatus;

/**
 * 邮件异常
 *
 * @author Fly
 * @since 2024-10-15 10:28
 */
public class UserErrorException extends BaseException {
    public UserErrorException() {
    }

    public UserErrorException(HttpStatus status, String msg) {
        super(status, msg);
    }

    public UserErrorException(HttpStatus status, String msg, Object data) {
        super(status, msg, data);
    }
}
