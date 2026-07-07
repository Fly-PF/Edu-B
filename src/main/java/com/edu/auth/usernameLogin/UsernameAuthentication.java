package com.edu.auth.usernameLogin;

import com.edu.auth.entity.UsernameLoginReq;
import com.edu.pojo.dto.UserInfoDTO;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * 描述
 *
 * @author Fly
 * @since 2026-03-17 12:09
 */


@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class UsernameAuthentication extends AbstractAuthenticationToken {
    private UsernameLoginReq usernameLoginReq;
    private UserInfoDTO userInfoDTO;

    public UsernameAuthentication(@Nullable Collection<GrantedAuthority> authorities) {
        super(authorities);
    }

    public UsernameAuthentication() {
        super((Collection<GrantedAuthority>) null);
    }

    public UsernameAuthentication(UsernameLoginReq usernameLoginReq, UserInfoDTO userInfoDTO,
                                  @Nullable Collection<GrantedAuthority> authorities
    ) {
        super(authorities);
        this.usernameLoginReq = usernameLoginReq;
        this.userInfoDTO = userInfoDTO;
    }

    public UsernameAuthentication(UsernameLoginReq usernameLoginReq, UserInfoDTO userInfoDTO) {
        this(usernameLoginReq, userInfoDTO, null);
    }

    @Override
    public @Nullable Object getPrincipal() {
        if (isAuthenticated()) {
            return userInfoDTO;
        }
        return usernameLoginReq;
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }
}
