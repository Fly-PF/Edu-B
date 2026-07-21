package com.edu.repository;

import org.springframework.web.multipart.MultipartFile;

public interface RagRepository {
    void uploadObject(MultipartFile file, String objectName);
}
