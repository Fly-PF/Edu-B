package com.edu.exception.handler;

import com.edu.common.Result;
import com.edu.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 *
 * @author Fly
 * @since 2024-10-10 15:55
 */

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public Result<?> exceptionHandler(BaseException ex, HttpServletRequest request) {
        log.error("业务异常 -> 请求路径：{}，异常信息：{}", request.getRequestURI(), ex.getMessage());
        HttpStatus status = ex.getStatus();
        return Result.setResult(status, ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handle404Exception(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("404异常 -> 请求路径：{}，异常栈：{}", request.getRequestURI(), ex.toString());
        return Result.setResult(HttpStatus.NOT_FOUND, "没有此资源！");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public Result<?> handle403Exception(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("403异常 -> 请求路径：{}，异常栈：{}", request.getRequestURI(), ex.toString());
        return Result.setResult(HttpStatus.FORBIDDEN, "权限认证失败！");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleAllException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("未知异常 -> 请求路径：{}，异常栈：{}", request.getRequestURI(), ex.toString());
        return Result.setResult(HttpStatus.BAD_REQUEST, ex.toString());
    }
}
