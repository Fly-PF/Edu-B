package com.edu.util;

import com.edu.pojo.dto.UserInfoDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    /**
     * 获取当前Authentication
     */
    public static Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取登录用户对象
     */
    public static UserInfoDTO getLoginUser() {
        Authentication auth = getAuth();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserInfoDTO userInfoDTO) {
            return userInfoDTO;
        }
        return null;
    }

    /**
     * 获取自定义登录用户对象
     */
    public static <T> T getLoginUser(Class<T> clazz) {
        Authentication auth = getAuth();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            return null;
        }

        // 类型匹配直接返回
        if (clazz.isInstance(principal)) {
            return clazz.cast(principal);
        }

        // 类型不匹配返回null
        return null;
    }

}
