package com.edu.repository.impl;

import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.repository.RagRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RagRepositoryImpl implements RagRepository {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void uploadObject(MultipartFile file, String objectName) {
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(getBucketName())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception ex) {
            log.error("上传RAG文件到MinIO失败，objectName={}", objectName, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "文件上传失败");
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

    private String getBucketName() {
        String bucketName = minioProperties.getBuckerName();
        if (!StringUtils.hasText(bucketName)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO存储桶未配置");
        }
        return bucketName;
    }
}
