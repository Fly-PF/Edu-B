package com.edu.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.personnel.CreatePersonnelRequest;
import com.edu.pojo.dto.personnel.UpdatePersonnelRequest;
import com.edu.pojo.po.SysRolePO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.po.SysUserRolePO;
import com.edu.pojo.vo.personnel.CreatePersonnelResultVO;
import com.edu.pojo.vo.personnel.PageResultVO;
import com.edu.pojo.vo.personnel.PersonnelVO;
import com.edu.repository.SysRoleRepository;
import com.edu.repository.SysUserRoleRepository;
import com.edu.repository.SysUserRepository;
import com.edu.service.PersonnelService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonnelServiceImpl implements PersonnelService {
    private static final int USER_TYPE_STUDENT = 1;
    private static final int USER_TYPE_TEACHER = 2;
    private static final int USER_TYPE_MANAGER = 4;
    private static final String ROLE_CODE_STUDENT = "STUDENT";
    private static final String ROLE_CODE_TEACHER = "TEACHER";
    private static final String ROLE_CODE_MANAGER = "ADMIN";
    private static final String CREATE_SUCCESS_MESSAGE = "新增成功";
    private static final String RESTORE_SUCCESS_MESSAGE = "账号存在，但之前已逻辑删除，现在已经覆盖恢复";
    private static final Set<Long> ALLOWED_PAGE_SIZES = Set.of(10L, 20L, 50L);
    private static final TypeReference<Map<String, Object>> EXT_JSON_TYPE = new TypeReference<>() {
    };

    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public PageResultVO<PersonnelVO> page(Integer userType, Long pageNum, Long pageSize, String keyword, Integer status) {
        validateUserType(userType);
        long currentPage = pageNum == null ? 1L : pageNum;
        long currentSize = pageSize == null ? 10L : pageSize;
        validatePage(currentPage, currentSize, status);

        IPage<SysUserPO> page = sysUserRepository.selectPersonnelPage(currentPage, currentSize, keyword, status, userType);
        return PageResultVO.<PersonnelVO>builder()
                .total(page.getTotal())
                .pageNum(currentPage)
                .pageSize(currentSize)
                .records(page.getRecords().stream().map(this::toVO).toList())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatePersonnelResultVO create(Integer userType, CreatePersonnelRequest request) {
        validateUserType(userType);
        validateExtJsonLength(request.getExtJson());
        SysUserPO existingUser = sysUserRepository.selectUserByUsername(request.getUsername());
        if (existingUser != null) {
            return restoreDeletedUser(existingUser, userType, request);
        }

        Long currentUserId = getCurrentUserId();
        SysUserPO user = buildPersonnel(request, userType, currentUserId);
        user.setCreateBy(currentUserId);
        sysUserRepository.insertUser(user);
        bindDefaultRole(user.getId(), userType);
        return CreatePersonnelResultVO.builder()
                .id(user.getId())
                .message(CREATE_SUCCESS_MESSAGE)
                .build();
    }

    @Override
    public PersonnelVO detail(Integer userType, Long id) {
        validateUserType(userType);
        return toVO(getPersonnelOrThrow(id, userType));
    }

    @Override
    public void update(Integer userType, Long id, UpdatePersonnelRequest request) {
        validateUserType(userType);
        getPersonnelOrThrow(id, userType);
        validateUpdateRequest(request);

        if (request.getPresentFields().isEmpty()) {
            return;
        }

        SysUserPO user = SysUserPO.builder()
                .userType(userType)
                .password(request.hasField("password") ? passwordEncoder.encode(request.getPassword()) : null)
                .realName(request.getRealName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .avatar(request.getAvatar())
                .grade(request.getGrade())
                .school(request.getSchool())
                .status(request.getStatus())
                .extJson(request.hasField("extJson") ? writeExtJson(request.getExtJson()) : null)
                .updateBy(getCurrentUserId())
                .build();
        sysUserRepository.updatePersonnelFields(id, user, request.getPresentFields());
    }

    @Override
    public String updateStatus(Integer userType, Long id, Integer status) {
        validateUserType(userType);
        getPersonnelOrThrow(id, userType);
        UpdatePersonnelRequest request = new UpdatePersonnelRequest();
        request.setStatus(status);
        SysUserPO user = SysUserPO.builder()
                .userType(userType)
                .status(status)
                .updateBy(getCurrentUserId())
                .build();
        sysUserRepository.updatePersonnelFields(id, user, request.getPresentFields());
        return status == 1 ? "启用成功" : "禁用成功";
    }

    @Override
    public void delete(Integer userType, Long id) {
        validateUserType(userType);
        getPersonnelOrThrow(id, userType);
        SysUserPO user = SysUserPO.builder()
                .id(id)
                .deleted(1)
                .updateBy(getCurrentUserId())
                .build();
        sysUserRepository.updateUserById(user);
    }

    private void validatePage(long pageNum, long pageSize, Integer status) {
        if (pageNum < 1 || !ALLOWED_PAGE_SIZES.contains(pageSize) || (status != null && status != 0 && status != 1)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请求参数错误");
        }
    }

    private void validateUserType(Integer userType) {
        if (userType == null || (userType != USER_TYPE_STUDENT && userType != USER_TYPE_TEACHER && userType != USER_TYPE_MANAGER)) {
            throw new BaseException(HttpStatus.NOT_FOUND, "人员不存在");
        }
    }

    private SysUserPO getPersonnelOrThrow(Long id, Integer userType) {
        SysUserPO user = sysUserRepository.selectPersonnelById(id, userType);
        if (user == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "人员不存在");
        }
        return user;
    }

    private void validateUpdateRequest(UpdatePersonnelRequest request) {
        if (request.getUnknownFields().contains("username")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "账号不允许修改");
        }
        if (request.hasField("password") && !StringUtils.hasText(request.getPassword())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "密码不能为空");
        }
        if (request.hasField("realName") && !StringUtils.hasText(request.getRealName())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "姓名不能为空");
        }
        if (request.hasField("phone") && !StringUtils.hasText(request.getPhone())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "手机号不能为空");
        }
        if (request.hasField("status") && request.getStatus() == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "状态不能为空");
        }
        if (request.hasField("extJson")) {
            validateExtJsonLength(request.getExtJson());
        }
    }

    private Long getCurrentUserId() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        return loginUser == null ? null : loginUser.getUserId();
    }

    private CreatePersonnelResultVO restoreDeletedUser(SysUserPO existingUser, Integer userType, CreatePersonnelRequest request) {
        if (existingUser.getDeleted() == null || existingUser.getDeleted() == 0 || !Objects.equals(existingUser.getUserType(), userType)) {
            throw new BaseException(HttpStatus.CONFLICT, "账号已存在");
        }

        SysUserPO restoreUser = buildPersonnel(request, userType, getCurrentUserId());
        restoreUser.setId(existingUser.getId());
        int rows = sysUserRepository.restoreDeletedPersonnel(restoreUser);
        if (rows != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器异常");
        }
        bindDefaultRole(existingUser.getId(), userType);
        return CreatePersonnelResultVO.builder()
                .id(existingUser.getId())
                .message(RESTORE_SUCCESS_MESSAGE)
                .build();
    }

    private SysUserPO buildPersonnel(CreatePersonnelRequest request, Integer userType, Long currentUserId) {
        return SysUserPO.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .avatar(request.getAvatar())
                .userType(userType)
                .grade(request.getGrade())
                .school(request.getSchool())
                .status(request.getStatus() == null ? 1 : request.getStatus())
                .updateBy(currentUserId)
                .deleted(0)
                .extJson(writeExtJson(request.getExtJson()))
                .build();
    }

    private PersonnelVO toVO(SysUserPO user) {
        return PersonnelVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .userType(user.getUserType())
                .userTypeName(userTypeName(user.getUserType()))
                .grade(user.getGrade())
                .school(user.getSchool())
                .status(user.getStatus())
                .statusName(user.getStatus() != null && user.getStatus() == 1 ? "正常" : "禁用")
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .extJson(readExtJson(user.getExtJson()))
                .build();
    }

    private String userTypeName(Integer userType) {
        if (userType == USER_TYPE_STUDENT) {
            return "学生";
        }
        if (userType == USER_TYPE_TEACHER) {
            return "教师";
        }
        if (userType == USER_TYPE_MANAGER) {
            return "平台管理员";
        }
        return "未知";
    }

    private void bindDefaultRole(Long userId, Integer userType) {
        String roleCode = roleCode(userType);
        SysRolePO role = sysRoleRepository.selectRoleByCode(roleCode);
        if (role == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器异常");
        }
        SysUserRolePO userRole = SysUserRolePO.builder()
                .userId(userId)
                .roleId(role.getId())
                .build();
        if (sysUserRoleRepository.existsByUserIdAndRoleId(userId, role.getId())) {
            return;
        }
        int rows = sysUserRoleRepository.insertUserRole(userRole);
        if (rows != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器异常");
        }
    }

    private String roleCode(Integer userType) {
        if (userType == USER_TYPE_STUDENT) {
            return ROLE_CODE_STUDENT;
        }
        if (userType == USER_TYPE_TEACHER) {
            return ROLE_CODE_TEACHER;
        }
        if (userType == USER_TYPE_MANAGER) {
            return ROLE_CODE_MANAGER;
        }
        throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器异常");
    }

    private Map<String, Object> readExtJson(String extJson) {
        if (!StringUtils.hasText(extJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(extJson, EXT_JSON_TYPE);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    private String writeExtJson(Map<String, Object> extJson) {
        Map<String, Object> value = extJson == null ? Collections.emptyMap() : extJson;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "扩展信息格式错误");
        }
    }

    private void validateExtJsonLength(Map<String, Object> extJson) {
        String json = writeExtJson(extJson);
        if (json.length() > 2000) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "扩展信息最多2000个字符");
        }
    }
}
