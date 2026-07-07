package com.edu.auth.mailLogin;

import com.edu.auth.entity.MailLoginReq;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.auth.usernameLogin.UsernameAuthentication;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.SysUserRepository;
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

        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId(sysUserPO.getId())
                .username(sysUserPO.getUsername())
                .realName(sysUserPO.getRealName())
                .email(sysUserPO.getEmail())
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
