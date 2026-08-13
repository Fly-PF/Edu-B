package com.edu.auth.usernameLogin;

import com.edu.pojo.po.SysRolePO;
import com.edu.pojo.po.SysUserRolePO;
import com.edu.repository.SysRoleRepository;
import com.edu.repository.SysUserRepository;
import com.edu.auth.entity.UsernameLoginReq;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:14
 */
@Component
@RequiredArgsConstructor
public class UsernameAuthenticationProvider implements AuthenticationProvider {

    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRoleRepository sysRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public @Nullable Authentication authenticate(
            @NonNull Authentication authentication
    ) throws AuthenticationException {
        Object object = authentication.getPrincipal();

        if(object instanceof UserInfoDTO userInfoDTO){
            UsernameAuthentication userAuthenticated = new UsernameAuthentication();
            userAuthenticated.setUserInfoDTO(userInfoDTO);
            userAuthenticated.setAuthenticated(true);
            return userAuthenticated;
        }

        UsernameLoginReq usernameLoginReq = (UsernameLoginReq) object;
        if (usernameLoginReq == null) {
            throw new BadCredentialsException("用户登录请求参数错误！");
        }

        SysUserPO sysUserPO = sysUserRepository.selectUserByUsername(usernameLoginReq.getUsername());
        if(sysUserPO == null || sysUserPO.getDeleted() == 1){
            throw new BadCredentialsException("用户不存在！");
        }
        if(!passwordEncoder.matches(usernameLoginReq.getPassword(), sysUserPO.getPassword())){
            throw new BadCredentialsException("密码错误！");
        }

        if(sysUserPO.getStatus() == 0){
            throw new BadCredentialsException("账户已被禁用，请联系管理员！");
        }

        SysUserRolePO sysUserRolePO = sysUserRoleRepository.selectUserRoleByUserId(sysUserPO.getId());
        if (sysUserRolePO == null) {
            throw new BadCredentialsException("该用户没有分配角色，请联系管理员！");
        }
        SysRolePO sysRolePO = sysRoleRepository.selectRoleById(sysUserRolePO.getRoleId());

        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId(sysUserPO.getId())
                .username(sysUserPO.getUsername())
                .realName(sysUserPO.getRealName())
                .grade(sysUserPO.getGrade())
                .roleCode(sysRolePO.getRoleCode())
                .roleName(sysRolePO.getRoleName())
                .build();

        UsernameAuthentication userAuthenticated = new UsernameAuthentication();
        userAuthenticated.setUserInfoDTO(userInfoDTO);
        userAuthenticated.setAuthenticated(true);
        return userAuthenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(UsernameAuthentication.class);
    }
}
