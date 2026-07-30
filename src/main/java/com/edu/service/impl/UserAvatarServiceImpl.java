package com.edu.service.impl;

import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.SysUserPO;
import com.edu.repository.SysUserRepository;
import com.edu.service.UserAvatarService;
import com.edu.util.AvatarUrlBuilder;
import com.edu.util.SecurityUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarServiceImpl implements UserAvatarService {
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final SysUserRepository sysUserRepository;
    private final AvatarUrlBuilder avatarUrlBuilder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(MultipartFile file) {
        SysUserPO user = getCurrentUserOrThrow();
        return uploadAvatar(user.getId(), file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        validateAvatar(file);
        SysUserPO user = getUserOrThrow(userId);
        String oldAvatar = user.getAvatar();
        String newAvatar = buildObjectName(user.getId(), file);

        uploadObject(file, newAvatar);
        removeOldAvatarOrRollback(oldAvatar, newAvatar);
        updateAvatarOrThrow(user.getId(), newAvatar);
        return newAvatar;
    }

    @Override
    public String getAvatar() {
        SysUserPO user = getCurrentUserOrThrow();
        return avatarUrlBuilder.build(user.getAvatar());
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "头像文件不能超过5MB");
        }

        String extension = getExtension(file);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "头像仅支持jpg、jpeg、png、webp格式");
        }

        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "头像仅支持jpg、jpeg、png、webp格式");
        }
    }

    private SysUserPO getCurrentUserOrThrow() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return getUserOrThrow(loginUser.getUserId());
    }

    private SysUserPO getUserOrThrow(Long userId) {
        if (userId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "用户ID不能为空");
        }

        SysUserPO user = sysUserRepository.selectUserById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BaseException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void updateAvatarOrThrow(Long userId, String avatar) {
        int rows = sysUserRepository.updateAvatarById(userId, avatar);
        if (rows != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "头像文件已上传且旧头像已处理，但数据库头像地址更新失败");
        }
    }

    private void uploadObject(MultipartFile file, String objectName) {
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(getBucketName())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception ex) {
            log.error("上传头像到MinIO失败，objectName={}", objectName, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "头像上传失败");
        }
    }

    private void ensureBucketExists() throws Exception {
        String bucketName = getBucketName();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private void removeOldAvatarOrRollback(String oldAvatar, String newAvatar) {
        if (!shouldRemoveOldAvatar(oldAvatar)) {
            return;
        }

        try {
            removeObject(oldAvatar);
        } catch (Exception ex) {
            log.error("删除旧头像失败，oldAvatar={}", oldAvatar, ex);
            boolean rollbackSuccess = removeObjectQuietly(newAvatar);
            String message = rollbackSuccess
                    ? "旧头像删除失败，已删除新头像，数据库仍保持旧头像地址"
                    : "旧头像删除失败，新头像清理失败，数据库仍保持旧头像地址";
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }

    private boolean shouldRemoveOldAvatar(String objectName) {
        if (!StringUtils.hasText(objectName) || getDefaultAvatar().equals(objectName)) {
            return false;
        }
        return objectName.startsWith(getAvatarFilesBaseUrl());
    }

    private void removeObject(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(getBucketName())
                .object(objectName)
                .build());
    }

    private boolean removeObjectQuietly(String objectName) {
        try {
            removeObject(objectName);
            return true;
        } catch (Exception ex) {
            log.warn("删除MinIO头像失败，objectName={}", objectName, ex);
            return false;
        }
    }

    private String buildObjectName(Long userId, MultipartFile file) {
        return getAvatarFilesBaseUrl() + userId + "/" + UUID.randomUUID() + "." + getExtension(file);
    }

    private String getExtension(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String getBucketName() {
        String bucketName = minioProperties.getBuckerName();
        if (!StringUtils.hasText(bucketName)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        }
        return bucketName;
    }

    private String getDefaultAvatar() {
        String defaultAvatar = minioProperties.getAvatar().getDefaultAvatar();
        if (!StringUtils.hasText(defaultAvatar)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "默认头像未配置");
        }
        return trimStartSlash(defaultAvatar);
    }

    private String getAvatarFilesBaseUrl() {
        String avatarFilesBaseUrl = minioProperties.getAvatar().getAvatarFilesBaseUrl();
        if (!StringUtils.hasText(avatarFilesBaseUrl)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "头像存储路径未配置");
        }
        avatarFilesBaseUrl = trimStartSlash(avatarFilesBaseUrl);
        return avatarFilesBaseUrl.endsWith("/") ? avatarFilesBaseUrl : avatarFilesBaseUrl + "/";
    }

    private String trimStartSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
