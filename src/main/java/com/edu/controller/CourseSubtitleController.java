package com.edu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves WebVTT subtitle files with the MIME type required by browser video players.
 */
@RestController
public class CourseSubtitleController {

    private static final MediaType WEB_VTT = new MediaType("text", "vtt", StandardCharsets.UTF_8);

    @Value("${edu.course-storage.local-path:uploads}")
    private String courseStoragePath;

    @GetMapping(value = "/api/course-files/course/{courseId}/{fileName:.+\\.vtt}")
    public ResponseEntity<Resource> getSubtitle(
            @PathVariable String courseId,
            @PathVariable String fileName) {
        Path storageRoot = Paths.get(courseStoragePath).toAbsolutePath().normalize();
        Path subtitlePath = storageRoot.resolve("course")
                .resolve(courseId)
                .resolve(fileName)
                .normalize();

        if (!subtitlePath.startsWith(storageRoot) || !subtitlePath.toFile().isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, WEB_VTT.toString())
                .body(new FileSystemResource(subtitlePath));
    }
}
