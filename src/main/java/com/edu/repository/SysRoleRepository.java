package com.edu.repository;

import com.edu.pojo.po.SysRolePO;

public interface SysRoleRepository {
    SysRolePO selectRoleByCode(String roleCode);

    SysRolePO selectRoleById(Long roleId);
}
