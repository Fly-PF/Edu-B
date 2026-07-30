package com.edu.controller;

import com.edu.auth.entity.MailLoginReq;
import com.edu.auth.entity.StudentRegisterReq;
import com.edu.auth.entity.UsernameLoginReq;
import com.edu.common.Result;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.po.SysRolePO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.po.SysUserRolePO;
import com.edu.repository.SysRoleRepository;
import com.edu.repository.SysUserRepository;
import com.edu.repository.SysUserRoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "认证管理")
public class AuthController {
    private static final int USER_TYPE_STUDENT = 1;
    private static final String ROLE_CODE_STUDENT = "STUDENT";

    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioProperties minioProperties;

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

    @Operation(summary = "学生注册接口")
    @PostMapping("/api/user/register/student")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> registerStudent(@Valid @RequestBody StudentRegisterReq request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "两次密码不一致");
        }
        if (sysUserRepository.existsByUsername(request.getUsername())) {
            throw new BaseException(HttpStatus.CONFLICT, "账号已存在");
        }
        SysRolePO role = getStudentRole();

        SysUserPO user = SysUserPO.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName())
                .avatar(resolveDefaultAvatar())
                .userType(USER_TYPE_STUDENT)
                .status(1)
                .build();

        try {
            sysUserRepository.insertUser(user);
        } catch (DuplicateKeyException ex) {
            throw new BaseException(HttpStatus.CONFLICT, "账号已存在");
        }

        bindStudentRole(user.getId(), role.getId());
        return Result.setResult(HttpStatus.OK, "注册成功！", null);
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

    private String resolveDefaultAvatar() {
        String defaultAvatar = minioProperties.getAvatar().getDefaultAvatar();
        if (!StringUtils.hasText(defaultAvatar)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "默认头像未配置");
        }
        return StringUtils.trimLeadingCharacter(defaultAvatar, '/');
    }

    private SysRolePO getStudentRole() {
        SysRolePO role = sysRoleRepository.selectRoleByCode(ROLE_CODE_STUDENT);
        if (role == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器异常");
        }
        return role;
    }

    private void bindStudentRole(Long userId, Long roleId) {
        if (sysUserRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            return;
        }

        SysUserRolePO userRole = SysUserRolePO.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        int rows = sysUserRoleRepository.insertUserRole(userRole);
        if (rows != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器异常");
        }
    }
}
