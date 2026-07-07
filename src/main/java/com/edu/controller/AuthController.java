package com.edu.controller;

import com.edu.auth.entity.MailLoginReq;
import com.edu.common.Result;
import com.edu.auth.entity.UsernameLoginReq;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:19
 */
@Slf4j
@RestController
@RequestMapping
@Tag(name = "认证管理")
public class AuthController {
    @Operation(summary = "用户名登录接口")
    @PostMapping("/user/login/username")
    public Result<?> usernameLogin(@RequestBody UsernameLoginReq usernameLoginReq) {
        log.info("======usernameLogin in Controller: {}", usernameLoginReq);
        return Result.setResult(HttpStatus.OK, "success", usernameLoginReq);
    }

    @Operation(summary = "邮箱登录接口")
    @PostMapping("/user/login/mail")
    public Result<?> mailLogin(@RequestBody MailLoginReq mailLoginReq) {
        log.info("======mailLogin in Controller: {}", mailLoginReq);
        return Result.setResult(HttpStatus.OK, "success", mailLoginReq);
    }

    @Operation(summary = "api接口1")
    @PostMapping("/api/test1")
    public Result<?> apiAccess1() {
        log.info("======apiAccess1 in Controller");
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        return Result.setResult(HttpStatus.OK, "success", loginUser);
    }

    @Operation(summary = "api接口2")
    @PostMapping("/api/test2")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> apiAccess2() {
        log.info("======apiAccess2 in Controller");
        return Result.setResult(HttpStatus.OK, "success");
    }

    @Operation(summary = "测试接口1")
    @GetMapping("/test1")
    public Result<?> test1() {
        log.info("======test1 in Controller");
        return Result.setResult(HttpStatus.OK);
    }

    @Operation(summary = "测试接口2")
    @GetMapping("/test2")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> test2() {
        log.info("======test2 in Controller");
        return Result.setResult(HttpStatus.OK);
    }
}
