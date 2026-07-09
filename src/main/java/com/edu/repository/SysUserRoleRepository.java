package com.edu.repository;

import com.edu.pojo.po.SysUserRolePO;

public interface SysUserRoleRepository {
    int insertUserRole(SysUserRolePO userRole);
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    SysUserRolePO selectUserRoleByUserId(Long userId);
}
