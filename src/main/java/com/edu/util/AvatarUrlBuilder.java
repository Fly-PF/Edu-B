package com.edu.util;

import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AvatarUrlBuilder {
    private static final String AVATAR_IMAGE_PATH = "/api/user/avatar/image";
    private static final String OBJECT_NAME_PARAM = "objectName=";

    private final MinioProperties minioProperties;

    public String build(String avatar) {
        String objectName = resolveObjectName(avatar);
        String publicBaseUrl = trimEndSlash(minioProperties.getPublicBaseUrl());
        if (!StringUtils.hasText(publicBaseUrl) || !StringUtils.hasText(objectName)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO配置错误");
        }
        return publicBaseUrl + AVATAR_IMAGE_PATH + "?objectName=" + URLEncoder.encode(objectName, StandardCharsets.UTF_8);
    }

    public String getDefaultAvatarObjectName() {
        String defaultAvatar = minioProperties.getDefaultAvatar();
        if (!StringUtils.hasText(defaultAvatar)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "默认头像未配置");
        }
        return trimStartSlash(defaultAvatar);
    }

    private String resolveObjectName(String avatar) {
        String objectName = StringUtils.hasText(avatar) ? avatar.trim() : getDefaultAvatarObjectName();
        String objectNameParam = extractObjectNameParam(objectName);
        if (StringUtils.hasText(objectNameParam)) {
            objectName = objectNameParam;
        }
        return trimStartSlash(objectName);
    }

    private String extractObjectNameParam(String value) {
        int index = value.indexOf(OBJECT_NAME_PARAM);
        if (index < 0) {
            return "";
        }
        String paramValue = value.substring(index + OBJECT_NAME_PARAM.length());
        int endIndex = paramValue.indexOf('&');
        if (endIndex >= 0) {
            paramValue = paramValue.substring(0, endIndex);
        }
        return URLDecoder.decode(paramValue, StandardCharsets.UTF_8);
    }

    private String trimEndSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimStartSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
