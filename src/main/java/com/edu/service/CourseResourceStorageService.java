package com.edu.service;

import org.springframework.web.multipart.MultipartFile;

public interface CourseResourceStorageService {
    StoredCourseFile upload(Long courseId, MultipartFile file);

    String createReadUrl(String storedUrl);

    /**
     * Reads a local course file for server-side processing. External URLs are never fetched here.
     */
    byte[] readLocalBytes(String storedUrl, long maxBytes);

    void delete(String storedUrl);

    record StoredCourseFile(String objectName, String originalName, Long size, Integer resourceType) {
    }
}
