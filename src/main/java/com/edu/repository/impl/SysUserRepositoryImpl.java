package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.mapper.SysUserMapper;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;


@Repository
@RequiredArgsConstructor
public class SysUserRepositoryImpl implements SysUserRepository {
    private final SysUserMapper sysUserMapper;

    @Override
    public SysUserPO selectUserByUsername(String username) {
        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getUsername, username);
        return sysUserMapper.selectOne(queryWrapper);
    }

    @Override
    public SysUserPO selectUserByEmail(String email) {
        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getEmail, email);
        return sysUserMapper.selectOne(queryWrapper);
    }

    @Override
    public SysUserPO selectUserById(Long userId) {
        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getId, userId);
        return sysUserMapper.selectOne(queryWrapper);
    }

    @Override
    public IPage<SysUserPO> selectPersonnelPage(long pageNum, long pageSize, String keyword, Integer status, Integer userType) {
        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getDeleted, 0)
                .eq(SysUserPO::getUserType, userType)
                .eq(status != null, SysUserPO::getStatus, status)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SysUserPO::getUsername, keyword)
                        .or()
                        .like(SysUserPO::getRealName, keyword))
                .orderByDesc(SysUserPO::getCreateTime)
                .orderByDesc(SysUserPO::getId);
        return sysUserMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    @Override
    public SysUserPO selectPersonnelById(Long userId, Integer userType) {
        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getId, userId)
                .eq(SysUserPO::getUserType, userType)
                .eq(SysUserPO::getDeleted, 0);
        return sysUserMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getUsername, username);
        return sysUserMapper.exists(queryWrapper);
    }

    @Override
    public int insertUser(SysUserPO user) {
        return sysUserMapper.insert(user);
    }

    @Override
    public int updateUserById(SysUserPO user) {
        return sysUserMapper.updateById(user);
    }

    @Override
    public int updatePasswordById(Long userId, String password) {
        UpdateWrapper<SysUserPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .eq("deleted", 0)
                .set("password", password)
                .set("update_by", userId);
        return sysUserMapper.update(updateWrapper);
    }

    @Override
    public int updateAvatarById(Long userId, String avatar) {
        UpdateWrapper<SysUserPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .eq("deleted", 0)
                .set("avatar", avatar)
                .set("update_by", userId);
        return sysUserMapper.update(updateWrapper);
    }

    @Override
    public int updateLastLoginInfo(Long userId, String lastLoginIp, LocalDateTime lastLoginTime) {
        UpdateWrapper<SysUserPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .eq("deleted", 0)
                .set("last_login_ip", lastLoginIp)
                .set("last_login_time", lastLoginTime);
        return sysUserMapper.update(updateWrapper);
    }

    @Override
    public int updateProfileFields(Long userId, SysUserPO user, Set<String> fields) {
        UpdateWrapper<SysUserPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .eq("deleted", 0);

        if (fields.contains("realName")) {
            updateWrapper.set("real_name", user.getRealName());
        }
        if (fields.contains("phone")) {
            updateWrapper.set("phone", user.getPhone());
        }
        if (fields.contains("email")) {
            updateWrapper.set("email", user.getEmail());
        }
        if (fields.contains("grade")) {
            updateWrapper.set("grade", user.getGrade());
        }
        if (fields.contains("school")) {
            updateWrapper.set("school", user.getSchool());
        }
        if (user.getUpdateBy() != null) {
            updateWrapper.set("update_by", user.getUpdateBy());
        }
        return sysUserMapper.update(updateWrapper);
    }

    @Override
    public int updatePersonnelFields(Long userId, SysUserPO user, Set<String> fields) {
        UpdateWrapper<SysUserPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                .eq("user_type", user.getUserType())
                .eq("deleted", 0);

        if (fields.contains("password")) {
            updateWrapper.set("password", user.getPassword());
        }
        if (fields.contains("realName")) {
            updateWrapper.set("real_name", user.getRealName());
        }
        if (fields.contains("phone")) {
            updateWrapper.set("phone", user.getPhone());
        }
        if (fields.contains("email")) {
            updateWrapper.set("email", user.getEmail());
        }
        if (fields.contains("avatar")) {
            updateWrapper.set("avatar", user.getAvatar());
        }
        if (fields.contains("grade")) {
            updateWrapper.set("grade", user.getGrade());
        }
        if (fields.contains("school")) {
            updateWrapper.set("school", user.getSchool());
        }
        if (fields.contains("status")) {
            updateWrapper.set("status", user.getStatus());
        }
        if (fields.contains("extJson")) {
            updateWrapper.set("ext_json", user.getExtJson());
        }
        if (user.getUpdateBy() != null) {
            updateWrapper.set("update_by", user.getUpdateBy());
        }
        return sysUserMapper.update(updateWrapper);
    }

    @Override
    public int restoreDeletedPersonnel(SysUserPO user) {
        UpdateWrapper<SysUserPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", user.getId())
                .eq("user_type", user.getUserType())
                .eq("deleted", 1)
                .set("password", user.getPassword())
                .set("real_name", user.getRealName())
                .set("phone", user.getPhone())
                .set("email", user.getEmail())
                .set("avatar", user.getAvatar())
                .set("grade", user.getGrade())
                .set("school", user.getSchool())
                .set("status", user.getStatus())
                .set("deleted", 0)
                .set("ext_json", user.getExtJson());
        if (user.getUpdateBy() != null) {
            updateWrapper.set("update_by", user.getUpdateBy());
        }
        return sysUserMapper.update(updateWrapper);
    }
}
