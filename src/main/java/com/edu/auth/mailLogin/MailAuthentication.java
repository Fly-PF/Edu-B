package com.edu.auth.mailLogin;

import com.edu.auth.entity.MailLoginReq;
import com.edu.pojo.dto.UserInfoDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class MailAuthentication extends AbstractAuthenticationToken {
    private MailLoginReq mailLoginReq;
    private UserInfoDTO userInfoDTO;

    public MailAuthentication(@Nullable Collection<GrantedAuthority> authorities) {
        super(authorities);
    }

    public MailAuthentication() {
        super((Collection<GrantedAuthority>) null);
    }

    public MailAuthentication(MailLoginReq mailLoginReq, UserInfoDTO userInfoDTO,
                              @Nullable Collection<GrantedAuthority> authorities
    ) {
        super(authorities);
        this.mailLoginReq = mailLoginReq;
        this.userInfoDTO = userInfoDTO;
    }

    public MailAuthentication(MailLoginReq mailLoginReq, UserInfoDTO userInfoDTO) {
        this(mailLoginReq, userInfoDTO, null);
    }

    @Override
    public @Nullable Object getPrincipal() {
        if (isAuthenticated()) {
            return userInfoDTO;
        }
        return mailLoginReq;
    }

    @Override
    public @Nullable Object getCredentials() {
        return null;
    }
}
