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
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
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
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public void logicalDeleteVectorChunks(Long kbId, Long docId) {
        logicalDeleteVectorChunks("kb_id == " + kbId + " and doc_id == " + docId + " and deleted == 0");
    }

    @Override
    public void logicalDeleteKnowledgeBaseVectorChunks(Long kbId) {
        logicalDeleteVectorChunks("kb_id == " + kbId + " and deleted == 0");
    }

    private void logicalDeleteVectorChunks(String filter) {
        validateMilvusProperties();

        MilvusClientV2 client = null;
        try {
            client = new MilvusClientV2(ConnectConfig.builder()
                    .uri(milvusProperties.getEndpoint())
                    .token(milvusProperties.getToken())
                    .dbName(milvusProperties.getDatabaseName())
                    .build());
            String collectionName = milvusProperties.getRag().getCollectionName();
            QueryResp queryResp = client.query(QueryReq.builder()
                    .databaseName(milvusProperties.getDatabaseName())
                    .collectionName(collectionName)
                    .filter(filter)
                    .outputFields(List.of("id", "kb_id", "doc_id", "source_info", "content", "vector", "metadata", "create_time"))
                    .limit(16384)
                    .build());
            List<JsonObject> data = buildLogicalDeleteMilvusRows(queryResp);
            if (data.isEmpty()) {
                return;
            }

            try {
                client.upsert(UpsertReq.builder()
                        .databaseName(milvusProperties.getDatabaseName())
                        .collectionName(collectionName)
                        .data(data)
                        .build());
                client.flush(FlushReq.builder()
                        .collectionNames(List.of(collectionName))
                        .build());
            } catch (Exception ex) {
                restoreLogicalDeletedVectorChunks(client, collectionName, data, ex);
                throw ex;
            }
        } catch (Exception ex) {
            log.error("逻辑删除RAG向量失败，filter={}", filter, ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG向量删除失败");
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private void restoreLogicalDeletedVectorChunks(MilvusClientV2 client, String collectionName, List<JsonObject> data,
                                                   Exception originException) {
        try {
            for (JsonObject row : data) {
                row.addProperty("deleted", 0);
            }
            client.upsert(UpsertReq.builder()
                    .databaseName(milvusProperties.getDatabaseName())
                    .collectionName(collectionName)
                    .data(data)
                    .build());
            client.flush(FlushReq.builder()
                    .collectionNames(List.of(collectionName))
                    .build());
        } catch (Exception rollbackException) {
            originException.addSuppressed(rollbackException);
        }
    }

    @Override
    public List<RagSearchChunk> searchVectorChunks(float[] vector, List<Long> kbIds) {
        validateMilvusProperties();
        if (vector == null || kbIds == null || kbIds.isEmpty()) {
            return List.of();
        }

        IndexParam.MetricType metricType = resolveMetricType();
        MilvusClientV2 client = null;
        try {
            client = new MilvusClientV2(ConnectConfig.builder()
                    .uri(milvusProperties.getEndpoint())
                    .token(milvusProperties.getToken())
                    .dbName(milvusProperties.getDatabaseName())
                    .build());
            SearchResp searchResp = client.search(SearchReq.builder()
                    .databaseName(milvusProperties.getDatabaseName())
                    .collectionName(milvusProperties.getRag().getCollectionName())
                    .data(List.of(new FloatVec(vector)))
                    .annsField("vector")
                    .filter("deleted == 0 and kb_id in [" + kbIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]")
                    .outputFields(List.of("kb_id", "doc_id", "source_info", "content"))
                    .topK(defaultTopK())
                    .metricType(metricType)
                    .build());
            return buildSearchChunks(searchResp, metricType);
        } catch (Exception ex) {
            log.error("Milvus检索RAG片段失败", ex);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "RAG检索失败");
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

    private List<JsonObject> buildLogicalDeleteMilvusRows(QueryResp queryResp) {
        List<JsonObject> data = new ArrayList<>();
        if (queryResp == null || queryResp.getQueryResults() == null) {
            return data;
        }

        for (QueryResp.QueryResult result : queryResp.getQueryResults()) {
            Map<String, Object> entity = result.getEntity();
            if (entity == null || entity.get("id") == null) {
                continue;
            }
            JsonObject row = new JsonObject();
            addMilvusNumber(row, "id", entity.get("id"));
            addMilvusNumber(row, "kb_id", entity.get("kb_id"));
            addMilvusNumber(row, "doc_id", entity.get("doc_id"));
            row.addProperty("source_info", String.valueOf(entity.getOrDefault("source_info", "")));
            row.addProperty("content", String.valueOf(entity.getOrDefault("content", "")));
            row.add("vector", buildJsonArray(entity.get("vector")));
            row.add("metadata", buildJsonObject(entity.get("metadata")));
            row.addProperty("create_time", String.valueOf(entity.get("create_time")));
            row.addProperty("deleted", 1);
            data.add(row);
        }
        return data;
    }

    private List<RagSearchChunk> buildSearchChunks(SearchResp searchResp, IndexParam.MetricType metricType) {
        List<RagSearchChunk> chunks = new ArrayList<>();
        if (searchResp == null || searchResp.getSearchResults() == null) {
            return chunks;
        }

        for (Object item : searchResp.getSearchResults()) {
            if (item instanceof SearchResp.SearchResult result) {
                appendSearchChunk(chunks, result, metricType);
                continue;
            }
            if (item instanceof List<?> resultList) {
                for (Object resultItem : resultList) {
                    if (resultItem instanceof SearchResp.SearchResult result) {
                        appendSearchChunk(chunks, result, metricType);
                    }
                }
            }
        }
        return chunks;
    }

    private void appendSearchChunk(List<RagSearchChunk> chunks, SearchResp.SearchResult result, IndexParam.MetricType metricType) {
        if (result == null || !matchScore(result.getScore(), metricType)) {
            return;
        }
        Map<String, Object> entity = result.getEntity();
        if (entity == null) {
            return;
        }
        Long kbId = toLong(entity.get("kb_id"));
        Long docId = toLong(entity.get("doc_id"));
        if (kbId == null || docId == null) {
            return;
        }
        chunks.add(new RagSearchChunk(kbId, docId,
                String.valueOf(entity.getOrDefault("source_info", "")),
                String.valueOf(entity.getOrDefault("content", "")),
                result.getScore()));
    }

    private boolean matchScore(Float score, IndexParam.MetricType metricType) {
        if (score == null) {
            return false;
        }
        Double threshold = milvusProperties.getRag().getScoreThreshold();
        if (threshold == null) {
            return true;
        }
        return metricType == IndexParam.MetricType.L2 ? score <= threshold : score >= threshold;
    }

    private int defaultTopK() {
        Integer topK = milvusProperties.getRag().getTopK();
        return topK == null || topK < 1 ? 5 : topK;
    }

    private IndexParam.MetricType resolveMetricType() {
        String metricType = milvusProperties.getRag().getVector().getMetricType();
        if (!StringUtils.hasText(metricType)) {
            return IndexParam.MetricType.COSINE;
        }
        return IndexParam.MetricType.valueOf(metricType.toUpperCase());
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void addMilvusNumber(JsonObject row, String fieldName, Object value) {
        if (value instanceof Number number) {
            row.addProperty(fieldName, number);
            return;
        }
        row.addProperty(fieldName, Long.parseLong(String.valueOf(value)));
    }

    private JsonArray buildJsonArray(Object value) {
        JsonArray array = new JsonArray();
        if (value instanceof JsonArray jsonArray) {
            return jsonArray;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof Number number) {
                    array.add(number);
                }
            }
        }
        return array;
    }

    private JsonObject buildJsonObject(Object value) {
        if (value instanceof JsonObject jsonObject) {
            return jsonObject;
        }
        return new JsonObject();
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
