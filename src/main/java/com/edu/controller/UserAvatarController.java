package com.edu.controller;

import com.edu.common.Result;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.service.UserAvatarService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/avatar")
@Tag(name = "用户头像")
public class UserAvatarController {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final UserAvatarService userAvatarService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Operation(summary = "上传当前用户头像")
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String avatar = userAvatarService.uploadAvatar(file);
        return Result.setResult(HttpStatus.OK, "上传成功", Map.of("avatar", avatar));
    }

    @Operation(summary = "管理员上传指定用户头像")
    @PostMapping("/upload/{userId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPERADMIN')")
    public Result<Map<String, String>> uploadAvatarByUserId(@PathVariable Long userId,
                                                            @RequestPart("file") MultipartFile file) {
        String avatar = userAvatarService.uploadAvatar(userId, file);
        return Result.setResult(HttpStatus.OK, "上传成功", Map.of("avatar", avatar));
    }

    @Operation(summary = "获取当前用户头像")
    @GetMapping
    public Result<Map<String, String>> getAvatar() {
        return Result.setResult(HttpStatus.OK, "获取成功", Map.of("avatar", userAvatarService.getAvatar()));
    }

    @Operation(summary = "获取头像图片")
    @GetMapping("/image")
    public ResponseEntity<byte[]> getImage(@RequestParam String objectName) {
        validateObjectName(objectName);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(getBucketName())
                .object(objectName)
                .build())) {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            return ResponseEntity.ok()
                    .contentType(getMediaType(objectName))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(bytes);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.NOT_FOUND, "图片不存在");
        }
    }

    private void validateObjectName(String objectName) {
        if (!StringUtils.hasText(objectName) || objectName.contains("..")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "图片地址错误");
        }

        String extension = StringUtils.getFilenameExtension(objectName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅支持jpg、jpeg、png、webp格式图片");
        }
    }

    private MediaType getMediaType(String objectName) {
        String extension = StringUtils.getFilenameExtension(objectName);
        if (extension == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private String getBucketName() {
        String bucketName = minioProperties.getBuckerName();
        if (!StringUtils.hasText(bucketName)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        }
        return bucketName;
    }
}
