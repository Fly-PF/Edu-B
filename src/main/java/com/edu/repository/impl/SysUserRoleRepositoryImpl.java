package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.SysUserRoleMapper;
import com.edu.pojo.po.SysUserRolePO;
import com.edu.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SysUserRoleRepositoryImpl implements SysUserRoleRepository {
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public int insertUserRole(SysUserRolePO userRole) {
        return sysUserRoleMapper.insert(userRole);
    }

    @Override
    public boolean existsByUserIdAndRoleId(Long userId, Long roleId) {
        LambdaQueryWrapper<SysUserRolePO> queryWrapper = new LambdaQueryWrapper<SysUserRolePO>()
                .eq(SysUserRolePO::getUserId, userId)
                .eq(SysUserRolePO::getRoleId, roleId);
        return sysUserRoleMapper.exists(queryWrapper);
    }

    @Override
    public SysUserRolePO selectUserRoleByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRolePO> queryWrapper = new LambdaQueryWrapper<SysUserRolePO>()
                .eq(SysUserRolePO::getUserId, userId);
        return sysUserRoleMapper.selectOne(queryWrapper);
    }
}
