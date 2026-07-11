package com.edu.service;

import org.springframework.web.multipart.MultipartFile;

public interface CourseResourceStorageService {
    StoredCourseFile upload(Long courseId, MultipartFile file);

    String createReadUrl(String storedUrl);

    void delete(String storedUrl);

    record StoredCourseFile(String objectName, String originalName, Long size, Integer resourceType) {
    }
}
