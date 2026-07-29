package com.edu.service.impl;

import com.edu.exception.BaseException;
import com.edu.service.CourseResourceStorageService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class CourseResourceStorageServiceImpl implements CourseResourceStorageService {
    private static final long MAX_FILE_SIZE = 1024L * 1024L * 1024L;
    private static final String COURSE_DIR = "course/";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp4", "webm", "mov", "m4v",
            "pdf",
            "jpg", "jpeg", "png", "webp",
            "csv", "xls", "xlsx", "zip"
    );
    private static final Map<String, Integer> RESOURCE_TYPE_BY_EXTENSION = Map.ofEntries(
            Map.entry("mp4", 1),
            Map.entry("webm", 1),
            Map.entry("mov", 1),
            Map.entry("m4v", 1),
            Map.entry("pdf", 2),
            Map.entry("jpg", 3),
            Map.entry("jpeg", 3),
            Map.entry("png", 3),
            Map.entry("webp", 3),
            Map.entry("csv", 4),
            Map.entry("xls", 4),
            Map.entry("xlsx", 4),
            Map.entry("zip", 4)
    );

    private final Path uploadRoot;
    private final String publicBaseUrl;

    public CourseResourceStorageServiceImpl(
            @Value("${edu.course-storage.local-path:uploads}") String localPath,
            @Value("${edu.course-storage.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.uploadRoot = Paths.get(localPath).toAbsolutePath().normalize();
        this.publicBaseUrl = trimEndSlash(publicBaseUrl);
    }

    @Override
    public StoredCourseFile upload(Long courseId, MultipartFile file) {
        validateFile(file);
        String extension = extension(file.getOriginalFilename());
        String objectName = COURSE_DIR + courseId + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolveStoredPath(objectName);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("上传课程资源失败，objectName={}", objectName, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "课程资源上传失败");
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "course-resource." + extension;
        return new StoredCourseFile(objectName, originalName, file.getSize(), RESOURCE_TYPE_BY_EXTENSION.get(extension));
    }

    @Override
    public String createReadUrl(String storedUrl) {
        if (!StringUtils.hasText(storedUrl) || isExternalUrl(storedUrl)) {
            return storedUrl;
        }
        return publicBaseUrl + "/api/course-files/" + encodePath(storedUrl);
    }

    @Override
    public byte[] readLocalBytes(String storedUrl, long maxBytes) {
        if (!StringUtils.hasText(storedUrl) || isExternalUrl(storedUrl) || maxBytes <= 0) {
            return null;
        }
        try {
            Path target = resolveStoredPath(storedUrl);
            if (!Files.isRegularFile(target) || Files.size(target) > maxBytes) {
                return null;
            }
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            log.warn("读取课程资料失败，objectName={}", storedUrl, ex);
            return null;
        }
    }

    @Override
    public void delete(String storedUrl) {
        if (!StringUtils.hasText(storedUrl) || isExternalUrl(storedUrl) || !storedUrl.startsWith(COURSE_DIR)) {
            return;
        }
        try {
            Files.deleteIfExists(resolveStoredPath(storedUrl));
        } catch (IOException ex) {
            log.warn("删除课程资源失败，objectName={}", storedUrl, ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程资源不能超过1GB");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该文件格式");
        }
    }

    private Path resolveStoredPath(String storedUrl) {
        Path target = uploadRoot.resolve(storedUrl).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程资源路径不合法");
        }
        return target;
    }

    private String extension(String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private boolean isExternalUrl(String value) {
        String lowerValue = value.toLowerCase(Locale.ROOT);
        return lowerValue.startsWith("http://") || lowerValue.startsWith("https://");
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
}
