package com.edu.controller;

import com.edu.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-05-02
 */
@Slf4j
@RestController
@RequestMapping
@Tag(name = "Hello 接口")
public class HelloController {

    @Operation(summary = "Hello World 接口")
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.setResult(HttpStatus.OK, "success", "hello world");
    }

    @Operation(summary = "Ping 健康检查")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.setResult(HttpStatus.OK, "success", "pong");
    }

    @Operation(summary = "当前时间")
    @GetMapping("/now")
    public Result<Map<String, Object>> now() {
        return Result.setResult(HttpStatus.OK, "success", Map.of(
                "localDateTime", LocalDateTime.now().toString()
        ));
    }

    @Operation(summary = "Echo 回显")
    @GetMapping("/echo")
    public Result<String> echo(@RequestParam(defaultValue = "hello") String msg) {
        return Result.setResult(HttpStatus.OK, "success", msg);
    }

    @Operation(summary = "版本信息")
    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.setResult(HttpStatus.OK, "success", Map.of(
                "version", "1.0.0",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

}
