package com.edu.auth.jwtAuth;

import com.edu.common.properties.JwtProperties;
import com.edu.pojo.po.SysRolePO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.SysUserRolePO;
import com.edu.repository.SysRoleRepository;
import com.edu.repository.SysUserRepository;
import com.edu.repository.SysUserRoleRepository;
import com.edu.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * JWT认证过滤器（每次请求都执行）
 *
 * @author Fly
 * @since 2026-02-26 19:20
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysRoleRepository sysRoleRepository;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    private static final List<String> EXCLUDE_PATHS = List.of(
            "/doc.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/swagger-resources/**",
            "/api/user/register/student",
            "/api/user/avatar/image",
            "/api/course-files/**"
    );

    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/api/course-categories",
            "/api/course-categories/tags",
            "/api/ai-exhibit/overview",
            "/api/ai-exhibit/cases"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        if (EXCLUDE_PATHS.stream().anyMatch(path -> PATH_MATCHER.match(path, requestURI))) {
            return true;
        }

        return "GET".equalsIgnoreCase(request.getMethod())
                && PUBLIC_GET_PATHS.stream().anyMatch(path -> PATH_MATCHER.match(path, requestURI));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(jwtProperties.getJwtName());
        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }
        log.info("authorizationHeader: {}", authorizationHeader);

        Claims claims;
        try {
            String jwtToken = authorizationHeader.substring(7); // 去掉 "Bearer " 前缀
            claims = jwtUtil.parseJWT(jwtToken);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (claims == null) {
            filterChain.doFilter(request, response);
            return;
        }
        log.info("claims: {}", claims);

        Long userId = jwtUtil.getCustomClaim(claims, "userId", Long.class);
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("userId: {}", userId);
        SysUserPO sysUserPO = sysUserRepository.selectUserById(userId);
        if (sysUserPO == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SysUserRolePO sysUserRolePO = sysUserRoleRepository.selectUserRoleByUserId(sysUserPO.getId());
        SysRolePO sysRolePO = sysRoleRepository.selectRoleById(sysUserRolePO.getRoleId());

        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId(sysUserPO.getId())
                .username(sysUserPO.getUsername())
                .realName(sysUserPO.getRealName())
                .roleCode(sysRolePO.getRoleCode())
                .roleName(sysRolePO.getRoleName())
                .build();

        GrantedAuthority authority = new SimpleGrantedAuthority(sysRolePO.getRoleCode());
        Collection<GrantedAuthority> authorities = List.of(authority);

        JwtAuthentication authentication = new JwtAuthentication(userInfoDTO, authorities);
        authentication.setAuthenticated(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        log.info("userInfo: {}", userInfoDTO);
        filterChain.doFilter(request, response);
    }
}
