package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.UpdateUserPasswordRequest;
import com.edu.service.UserPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/password")
@Tag(name = "用户密码")
public class UserPasswordController {
    private final UserPasswordService userPasswordService;

    @Operation(summary = "修改当前用户密码")
    @PatchMapping
    public Result<Void> updatePassword(@Valid @RequestBody UpdateUserPasswordRequest request) {
        userPasswordService.updatePassword(request);
        return Result.setResult(HttpStatus.OK, "修改成功", null);
    }
}
