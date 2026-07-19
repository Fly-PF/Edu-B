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
public class AiExhibitStorageService {
    private static final long MAX_FILE_SIZE = 30L * 1024L * 1024L;
    private static final String AI_EXHIBIT_DIR = "ai-exhibit/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp",
            "pdf", "txt", "md", "csv",
            "py", "js", "json", "zip"
    );

    private final Path uploadRoot;
    private final String publicBaseUrl;

    public AiExhibitStorageService(
            @Value("${edu.course-storage.local-path:uploads}") String localPath,
            @Value("${edu.course-storage.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.uploadRoot = Paths.get(localPath).toAbsolutePath().normalize();
        this.publicBaseUrl = trimEndSlash(publicBaseUrl);
    }

    public StoredAiFile upload(Long caseId, MultipartFile file) {
        validateFile(file);
        String extension = extension(file.getOriginalFilename());
        String objectName = AI_EXHIBIT_DIR + caseId + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolveStoredPath(objectName);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("AI exhibit attachment upload failed, objectName={}", objectName, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI展馆附件上传失败");
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "ai-practice." + extension;
        return new StoredAiFile(objectName, createReadUrl(objectName), originalName);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "AI展馆附件不能超过30MB");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该附件格式");
        }
    }

    private String createReadUrl(String storedUrl) {
        return publicBaseUrl + "/api/course-files/" + encodePath(storedUrl);
    }

    private Path resolveStoredPath(String storedUrl) {
        Path target = uploadRoot.resolve(storedUrl).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "AI展馆附件路径不合法");
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

    public record StoredAiFile(String objectName, String readUrl, String originalName) {
    }
}
