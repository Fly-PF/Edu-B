package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.SysRoleMapper;
import com.edu.pojo.po.SysRolePO;
import com.edu.repository.SysRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SysRoleRepositoryImpl implements SysRoleRepository {
    private final SysRoleMapper sysRoleMapper;

    @Override
    public SysRolePO selectRoleByCode(String roleCode) {
        LambdaQueryWrapper<SysRolePO> queryWrapper = new LambdaQueryWrapper<SysRolePO>()
                .eq(SysRolePO::getRoleCode, roleCode)
                .eq(SysRolePO::getDeleted, 0);
        return sysRoleMapper.selectOne(queryWrapper);
    }

    @Override
    public SysRolePO selectRoleById(Long roleId) {
        LambdaQueryWrapper<SysRolePO> queryWrapper = new LambdaQueryWrapper<SysRolePO>()
                .eq(SysRolePO::getId, roleId)
                .eq(SysRolePO::getDeleted, 0);
        return sysRoleMapper.selectOne(queryWrapper);
    }
}
