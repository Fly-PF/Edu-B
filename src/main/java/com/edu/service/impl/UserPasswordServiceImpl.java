package com.edu.service.impl;

import com.edu.exception.BaseException;
import com.edu.pojo.dto.UpdateUserPasswordRequest;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.SysUserRepository;
import com.edu.service.UserPasswordService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPasswordServiceImpl implements UserPasswordService {
    private final SysUserRepository sysUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdateUserPasswordRequest request) {
        SysUserPO user = getCurrentUserOrThrow();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "旧密码错误");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "新密码不能与旧密码一致");
        }

        int rows = sysUserRepository.updatePasswordById(user.getId(), passwordEncoder.encode(request.getNewPassword()));
        if (rows != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "密码修改失败");
        }
    }

    private SysUserPO getCurrentUserOrThrow() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        SysUserPO user = sysUserRepository.selectUserById(loginUser.getUserId());
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BaseException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
