package com.edu.service.impl;

import com.edu.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class AiFaceStorageService {
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final String AI_FACE_DIR = "ai-face/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path uploadRoot;
    private final String publicBaseUrl;

    public AiFaceStorageService(
            @Value("${edu.course-storage.local-path:uploads}") String localPath,
            @Value("${edu.course-storage.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.uploadRoot = Paths.get(localPath).toAbsolutePath().normalize();
        this.publicBaseUrl = trimEndSlash(publicBaseUrl);
    }

    public StoredFaceFile upload(Long userId, String purpose, MultipartFile file) {
        validateFile(file);
        String extension = extension(file.getOriginalFilename());
        String objectName = AI_FACE_DIR + userId + "/" + purpose + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolveStoredPath(objectName);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Face image upload failed, objectName={}", objectName, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "人脸图片上传失败");
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "face-image." + extension;
        return new StoredFaceFile(objectName, createReadUrl(objectName), originalName);
    }

    public byte[] readBytes(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "图片路径不能为空");
        }
        Path path = resolveStoredPath(objectName);
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "读取人脸图片失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "上传图片不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "人脸图片不能超过10MB");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该图片格式");
        }
    }

    private String createReadUrl(String storedUrl) {
        return publicBaseUrl + "/api/course-files/" + encodePath(storedUrl);
    }

    private Path resolveStoredPath(String storedUrl) {
        Path target = uploadRoot.resolve(storedUrl).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "人脸图片路径不合法");
        }
        return target;
    }

    private String extension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private String trimEndSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public record StoredFaceFile(String objectName, String readUrl, String originalName) {
    }
}
