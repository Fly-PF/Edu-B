package com.edu.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagService {
    void uploadRagFile(HttpServletRequest request, MultipartFile file);

    String chatTest(String message, List<MultipartFile> files);

    float[] embeddingTest(String message);
}
