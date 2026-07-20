package com.edu.service.impl;

import com.edu.exception.BaseException;
import com.edu.pojo.dto.UpdateUserProfileRequest;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.vo.UserProfileVO;
import com.edu.repository.SysUserRepository;
import com.edu.service.UserProfileService;
import com.edu.util.AvatarUrlBuilder;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final SysUserRepository sysUserRepository;
    private final AvatarUrlBuilder avatarUrlBuilder;

    @Override
    public UserProfileVO getProfile() {
        return toVO(getCurrentUserOrThrow());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateUserProfileRequest request) {
        Long userId = getCurrentUserIdOrThrow();
        getCurrentUserOrThrow();
        validateUpdateRequest(request);
        if (request.getPresentFields().isEmpty()) {
            return;
        }

        SysUserPO user = SysUserPO.builder()
                .realName(request.getRealName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .grade(request.getGrade())
                .school(request.getSchool())
                .updateBy(userId)
                .build();
        int rows = sysUserRepository.updateProfileFields(userId, user, request.getPresentFields());
        if (rows != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "个人信息修改失败");
        }
    }

    private void validateUpdateRequest(UpdateUserProfileRequest request) {
        if (!request.getUnknownFields().isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请求参数错误");
        }
        validatePresentText(request.hasField("realName"), request.getRealName(), "真实姓名不能为空");
        validatePresentText(request.hasField("phone"), request.getPhone(), "手机号不能为空");
        validatePresentText(request.hasField("email"), request.getEmail(), "邮箱不能为空");
        validatePresentText(request.hasField("grade"), request.getGrade(), "学段不能为空");
        validatePresentText(request.hasField("school"), request.getSchool(), "学校名称不能为空");
    }

    private void validatePresentText(boolean present, String value, String message) {
        if (present && !StringUtils.hasText(value)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private SysUserPO getCurrentUserOrThrow() {
        Long userId = getCurrentUserIdOrThrow();
        SysUserPO user = sysUserRepository.selectUserById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BaseException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private Long getCurrentUserIdOrThrow() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return loginUser.getUserId();
    }

    private UserProfileVO toVO(SysUserPO user) {
        return UserProfileVO.builder()
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(avatarUrlBuilder.build(user.getAvatar()))
                .userType(user.getUserType())
                .grade(user.getGrade())
                .school(user.getSchool())
                .build();
    }
}
