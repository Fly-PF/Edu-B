package com.edu.controller;

import com.edu.common.Result;
import com.edu.pojo.dto.UpdateUserProfileRequest;
import com.edu.pojo.vo.UserProfileVO;
import com.edu.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/profile")
@Tag(name = "个人中心")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @Operation(summary = "查看个人信息")
    @GetMapping
    public Result<UserProfileVO> getProfile() {
        return Result.setResult(HttpStatus.OK, "查询成功", userProfileService.getProfile());
    }

    @Operation(summary = "修改个人信息")
    @PatchMapping
    public Result<Void> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        userProfileService.updateProfile(request);
        return Result.setResult(HttpStatus.OK, "修改成功", null);
    }
}
