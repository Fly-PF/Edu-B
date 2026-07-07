package com.edu.util;

import com.edu.common.properties.JwtProperties;
import com.edu.exception.JwtErrorException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JJWT 0.13.0 JWT工具类
 */
@Component
public class JwtUtil {

    @Resource
    private JwtProperties jwtProperties;

    /**
     * 获取加密密钥
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT Token
     *
     * @param claims 自定义载荷信息，如userId、username、role
     * @return token字符串
     */
    public String createJWT(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        long expireTime = now + jwtProperties.getExpireTime();
        SecretKey key = getSecretKey();

        return Jwts.builder()
                // 存入自定义载荷
                .claims(claims)
                // 签发时间
                .issuedAt(new Date(now))
                // 过期时间
                .expiration(new Date(expireTime))
                // 签名算法+密钥
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析token，获取全部载荷
     *
     * @param token jwt令牌
     * @return Claims 载荷对象
     */
    public Claims parseJWT(String token) {
        try {
            SecretKey key = getSecretKey();
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtErrorException(HttpStatus.UNAUTHORIZED, "token已过期，请重新登录");
        } catch (MalformedJwtException e) {
            throw new JwtErrorException(HttpStatus.UNAUTHORIZED, "token格式错误，非法令牌");
        } catch (IllegalArgumentException e) {
            throw new JwtErrorException(HttpStatus.UNAUTHORIZED, "token不能为空");
        } catch (Exception e) {
            throw new JwtErrorException(HttpStatus.UNAUTHORIZED, "token解析失败", e);
        }
    }

    /**
     * 校验token是否有效（不抛出异常，返回布尔）
     */
    public boolean validateToken(String token) {
        try {
            parseJWT(token);
            return true;
        } catch (JwtErrorException e) {
            return false;
        }
    }

    public <T> T getCustomClaim(Claims claims, String key, Class<T> clazz) {
        if (claims == null || key == null || clazz == null) return null;
        return claims.get(key, clazz);
    }
}
