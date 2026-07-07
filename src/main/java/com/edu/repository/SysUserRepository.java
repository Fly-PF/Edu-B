package com.edu.repository;

import com.edu.pojo.po.SysUserPO;

public interface SysUserRepository {
    SysUserPO selectUserByUsername(String username);
    SysUserPO selectUserByEmail(String email);
    SysUserPO selectUserById(Long userId);
}
