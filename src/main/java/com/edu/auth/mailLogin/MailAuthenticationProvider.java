package com.edu.auth.mailLogin;

import com.edu.auth.entity.MailLoginReq;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.auth.usernameLogin.UsernameAuthentication;
import com.edu.pojo.po.SysRolePO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.po.SysUserRolePO;
import com.edu.repository.SysRoleRepository;
import com.edu.repository.SysUserRepository;
import com.edu.repository.SysUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailAuthenticationProvider implements AuthenticationProvider {
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRoleRepository sysRoleRepository;

    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Object object = authentication.getPrincipal();

        if(object instanceof UserInfoDTO userInfoDTO){
            UsernameAuthentication userAuthenticated = new UsernameAuthentication();
            userAuthenticated.setUserInfoDTO(userInfoDTO);
            userAuthenticated.setAuthenticated(true);
            return userAuthenticated;
        }

        MailLoginReq mailLoginReq = (MailLoginReq) object;
        if (mailLoginReq == null) {
            throw new BadCredentialsException("用户登录请求参数错误！");
        }

        SysUserPO sysUserPO = sysUserRepository.selectUserByEmail(mailLoginReq.getEmail());
        if(sysUserPO == null){
            throw new BadCredentialsException("用户不存在！");
        }

        if(mailLoginReq.getCaptchaKey().isEmpty() || mailLoginReq.getCaptchaValue().isEmpty()){
            throw new BadCredentialsException("验证码错误！");
        }

        SysUserRolePO sysUserRolePO = sysUserRoleRepository.selectUserRoleByUserId(sysUserPO.getId());
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
        return authentication.isAssignableFrom(MailAuthentication.class);
    }
}
