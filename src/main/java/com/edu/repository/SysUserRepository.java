package com.edu.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.pojo.po.SysUserPO;

import java.time.LocalDateTime;
import java.util.Set;

public interface SysUserRepository {
    SysUserPO selectUserByUsername(String username);
    SysUserPO selectUserByEmail(String email);
    SysUserPO selectUserById(Long userId);

    IPage<SysUserPO> selectPersonnelPage(long pageNum, long pageSize, String keyword, Integer status, Integer userType);

    SysUserPO selectPersonnelById(Long userId, Integer userType);

    boolean existsByUsername(String username);

    int insertUser(SysUserPO user);

    int updateUserById(SysUserPO user);

    int updatePasswordById(Long userId, String password);

    int updateAvatarById(Long userId, String avatar);

    int updateLastLoginInfo(Long userId, String lastLoginIp, LocalDateTime lastLoginTime);

    int updateProfileFields(Long userId, SysUserPO user, Set<String> fields);

    int updatePersonnelFields(Long userId, SysUserPO user, Set<String> fields);

    int restoreDeletedPersonnel(SysUserPO user);
}
