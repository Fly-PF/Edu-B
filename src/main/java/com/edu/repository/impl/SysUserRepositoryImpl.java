package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.mapper.SysUserMapper;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


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
}
