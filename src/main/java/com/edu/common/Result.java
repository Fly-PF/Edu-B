package com.edu.common;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;


@Data
public class Result<T> implements Serializable {

    private Integer code;  // 状态码
    private String message;  // 提示信息
    private T data;  // 数据

    public static <T> Result<T> setResult(HttpStatus httpStatus) {
        Result<T> result = new Result<>();
        result.code = httpStatus.value();
        result.message = "";
        result.data = null;
        return result;
    }

    public static <T> Result<T> setResult(HttpStatus httpStatus, String message) {
        Result<T> result = new Result<>();
        result.code = httpStatus.value();
        result.message = message;
        result.data = null;
        return result;
    }

    public static <T> Result<T> setResult(HttpStatus httpStatus, String message, T data) {
        Result<T> result = new Result<>();
        result.code = httpStatus.value();
        result.message = message;
        result.data = data;
        return result;
    }
}
