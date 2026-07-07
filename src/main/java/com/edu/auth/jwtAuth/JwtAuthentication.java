package com.edu.auth.jwtAuth;

import com.edu.pojo.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Slf4j
public class JwtAuthentication extends AbstractAuthenticationToken {
    private UserInfoDTO userInfoDTO;

    public JwtAuthentication(@Nullable Collection<GrantedAuthority> authorities) {
        super(authorities);
    }

    public JwtAuthentication() {
        super((Collection<GrantedAuthority>) null);
    }

    public JwtAuthentication(UserInfoDTO userInfoDTO,
                             @Nullable Collection<GrantedAuthority> authorities
    ) {
        super(authorities);
        this.userInfoDTO = userInfoDTO;
    }

    public JwtAuthentication(UserInfoDTO userInfoDTO) {
        this(userInfoDTO, null);
    }

    @Override
    public @Nullable Object getPrincipal() {
        return userInfoDTO;
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }
}
