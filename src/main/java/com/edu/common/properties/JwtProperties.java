package com.edu.common.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置读取
 */
@Component
@ConfigurationProperties(prefix = "edu.jwt")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtProperties {

    /**
     * jwt签名密钥
     */
    private String secretKey;

    /**
     * token过期时间 毫秒
     */
    private Long expireTime;

    /**
     * jwt名称
     */
    private String jwtName;
}
