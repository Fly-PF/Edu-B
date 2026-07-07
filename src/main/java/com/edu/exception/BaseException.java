package com.edu.exception;


import lombok.*;
import org.springframework.http.HttpStatus;

/**
 * 业务异常的基类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseException extends RuntimeException {

    private HttpStatus status;

    private Object data;

    public BaseException(HttpStatus status, String msg) {
        super(msg);
        this.status = status;
    }

    public BaseException(HttpStatus status, String msg, Object data) {
        super(msg);
        this.status = status;
        this.data = data;
    }
}
