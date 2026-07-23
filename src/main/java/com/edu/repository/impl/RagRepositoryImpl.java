package com.edu.repository.impl;

import com.edu.common.dto.RagVectorChunkDTO;
import com.edu.common.properties.MilvusProperties;
import com.edu.common.properties.MinioProperties;
import com.edu.exception.BaseException;
import com.edu.repository.RagRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RagRepositoryImpl implements RagRepository {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final MilvusProperties milvusProperties;

    @Override
    public void uploadObject(MultipartFile file, String objectName) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(buildPutObjectArgs(inputStream, file, objectName));
        } catch (Exception ex) {
            log.error("上传RAG文件到MinIO失败，objectName={}", objectName, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "文件上传失败");
        }
    }

    @Override
    public void deleteObject(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            log.warn("删除RAG文件失败，objectName={}", objectName, ex);
        }
    }

    @Override
    public void deleteObjectStrict(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception ex) {
            log.warn("删除RAG文件失败，objectName={}", objectName, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG文件回滚失败");
        }
    }

    @Override
    public void insertVectorChunks(List<RagVectorChunkDTO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        validateMilvusProperties();
        String createTime = OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString();
        List<JsonObject> data = buildMilvusRows(chunks, createTime);

        MilvusClientV2 client = null;
        try {
            client = new MilvusClientV2(ConnectConfig.builder()
                .uri(milvusProperties.getEndpoint())
                .token(milvusProperties.getToken())
                .dbName(milvusProperties.getDatabaseName())
                .build());
            String collectionName = milvusProperties.getRag().getCollectionName();
            client.insert(InsertReq.builder()
                    .databaseName(milvusProperties.getDatabaseName())
                    .collectionName(collectionName)
                    .data(data)
                    .build());
            client.flush(FlushReq.builder()
                    .collectionNames(List.of(collectionName))
                    .build());
            log.info("RAG向量已写入Milvus，collection={}, count={}", collectionName, data.size());
        } catch (Exception ex) {
            log.error("写入RAG向量到Milvus失败", ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG向量写入Milvus失败");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @Override
    public void deleteVectorChunks(Long kbId, Long docId) {
        validateMilvusProperties();

        MilvusClientV2 client = null;
        try {
            client = new MilvusClientV2(ConnectConfig.builder()
                    .uri(milvusProperties.getEndpoint())
                    .token(milvusProperties.getToken())
                    .dbName(milvusProperties.getDatabaseName())
                    .build());
            String collectionName = milvusProperties.getRag().getCollectionName();
            client.delete(DeleteReq.builder()
                    .databaseName(milvusProperties.getDatabaseName())
                    .collectionName(collectionName)
                    .filter("kb_id == " + kbId + " and doc_id == " + docId)
                    .build());
            client.flush(FlushReq.builder()
                    .collectionNames(List.of(collectionName))
                    .build());
        } catch (Exception ex) {
            log.error("删除RAG向量失败，kbId={}, docId={}", kbId, docId, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG向量回滚失败");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private JsonObject buildMilvusRow(RagVectorChunkDTO chunk, String createTime) {
        JsonObject row = new JsonObject();
        row.addProperty("kb_id", chunk.getKbId());
        row.addProperty("doc_id", chunk.getDocId());
        row.addProperty("source_info", chunk.getSourceInfo());
        row.addProperty("content", chunk.getContent());
        row.add("vector", buildVector(chunk.getVector()));
        row.add("metadata", new JsonObject());
        row.addProperty("create_time", createTime);
        row.addProperty("deleted", 0);
        return row;
    }

    private List<JsonObject> buildMilvusRows(List<RagVectorChunkDTO> chunks, String createTime) {
        List<JsonObject> data = new ArrayList<>();
        for (RagVectorChunkDTO chunk : chunks) {
            data.add(buildMilvusRow(chunk, createTime));
        }
        return data;
    }

    private JsonArray buildVector(float[] vector) {
        if (vector == null || vector.length != milvusProperties.getRag().getVector().getDimension()) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "向量维度与Milvus配置不一致");
        }

        JsonArray vectorData = new JsonArray();
        for (float value : vector) {
            vectorData.add(value);
        }
        return vectorData;
    }

    private PutObjectArgs buildPutObjectArgs(InputStream inputStream, MultipartFile file, String objectName)
            throws Exception {
        ensureBucketExists();
        return PutObjectArgs.builder()
                .bucket(getBucketName())
                .object(objectName)
                .stream(inputStream, file.getSize(), -1L)
                .contentType(file.getContentType())
                .build();
    }

    private void validateMilvusProperties() {
        if (!StringUtils.hasText(milvusProperties.getEndpoint())) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Milvus endpoint未配置");
        }
        if (!StringUtils.hasText(milvusProperties.getToken())) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Milvus token未配置");
        }
        if (!StringUtils.hasText(milvusProperties.getDatabaseName())) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Milvus database_name未配置");
        }
        if (milvusProperties.getRag() == null || !StringUtils.hasText(milvusProperties.getRag().getCollectionName())) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Milvus RAG collection_name未配置");
        }
        if (milvusProperties.getRag().getVector() == null || milvusProperties.getRag().getVector().getDimension() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Milvus RAG vector dimension未配置");
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