package com.edu.config;

import com.edu.common.properties.MilvusProperties;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MilvusSchemaInitializer implements ApplicationRunner {
    private static final String COLLECTION_DESCRIPTION = "RAG document chunk vectors";
    private static final String VECTOR_INDEX_NAME = "rag_chunk_vector_index";

    private final MilvusProperties milvusProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(milvusProperties.getEndpoint())
                || !StringUtils.hasText(milvusProperties.getDatabaseName())
                || milvusProperties.getRag() == null
                || !StringUtils.hasText(milvusProperties.getRag().getCollectionName())) {
            log.info("Skip Milvus schema initialization because configuration is incomplete.");
            return;
        }

        try {
            ensureDatabaseExists();
            ensureCollectionExists();
        } catch (Exception ex) {
            log.warn("Milvus schema initialization skipped: {}", ex.getMessage());
        }
    }

    private void ensureDatabaseExists() {
        MilvusClientV2 client = new MilvusClientV2(buildConnectConfig());
        try {
            String databaseName = milvusProperties.getDatabaseName();
            List<String> databases = client.listDatabases().getDatabaseNames();
            if (databases != null && databases.contains(databaseName)) {
                return;
            }
            client.createDatabase(CreateDatabaseReq.builder()
                    .databaseName(databaseName)
                    .build());
            log.info("Created missing Milvus database: {}", databaseName);
        } finally {
            client.close();
        }
    }

    private void ensureCollectionExists() {
        String databaseName = milvusProperties.getDatabaseName();
        String collectionName = milvusProperties.getRag().getCollectionName();
        MilvusClientV2 client = new MilvusClientV2(buildConnectConfig(databaseName));
        try {
            Boolean exists = client.hasCollection(HasCollectionReq.builder()
                    .databaseName(databaseName)
                    .collectionName(collectionName)
                    .build());
            if (!Boolean.TRUE.equals(exists)) {
                client.createCollection(CreateCollectionReq.builder()
                        .databaseName(databaseName)
                        .collectionName(collectionName)
                        .description(COLLECTION_DESCRIPTION)
                        .autoID(true)
                        .enableDynamicField(false)
                        .collectionSchema(buildSchema())
                        .indexParam(IndexParam.builder()
                                .fieldName("vector")
                                .indexName(VECTOR_INDEX_NAME)
                                .indexType(IndexParam.IndexType.AUTOINDEX)
                                .metricType(resolveMetricType())
                                .build())
                        .properties(Map.of("timezone", "Asia/Shanghai"))
                        .build());
                log.info("Created missing Milvus collection: {}.{}", databaseName, collectionName);
            }
            client.loadCollection(LoadCollectionReq.builder()
                    .databaseName(databaseName)
                    .collectionName(collectionName)
                    .build());
        } finally {
            client.close();
        }
    }


    private ConnectConfig buildConnectConfig() {
        var builder = ConnectConfig.builder()
                .uri(milvusProperties.getEndpoint());
        if (StringUtils.hasText(milvusProperties.getToken())) {
            builder.token(milvusProperties.getToken());
        }
        return builder.build();
    }

    private ConnectConfig buildConnectConfig(String databaseName) {
        var builder = ConnectConfig.builder()
                .uri(milvusProperties.getEndpoint())
                .dbName(databaseName);
        if (StringUtils.hasText(milvusProperties.getToken())) {
            builder.token(milvusProperties.getToken());
        }
        return builder.build();
    }
    private CreateCollectionReq.CollectionSchema buildSchema() {
        return CreateCollectionReq.CollectionSchema.builder()
                .enableDynamicField(false)
                .fieldSchemaList(List.of(
                        CreateCollectionReq.FieldSchema.builder()
                                .name("id")
                                .description("id")
                                .dataType(DataType.Int64)
                                .isPrimaryKey(true)
                                .autoID(true)
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("kb_id")
                                .description("knowledge base id")
                                .dataType(DataType.Int64)
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("doc_id")
                                .description("document id")
                                .dataType(DataType.Int64)
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("source_info")
                                .description("source info")
                                .dataType(DataType.VarChar)
                                .maxLength(256)
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("content")
                                .description("chunk content")
                                .dataType(DataType.VarChar)
                                .maxLength(65535)
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("vector")
                                .description("embedding vector")
                                .dataType(DataType.FloatVector)
                                .dimension(resolveDimension())
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("metadata")
                                .description("metadata")
                                .dataType(DataType.JSON)
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("create_time")
                                .description("create time")
                                .dataType(DataType.VarChar)
                                .maxLength(64)
                                .build(),
                        CreateCollectionReq.FieldSchema.builder()
                                .name("deleted")
                                .description("logical delete flag")
                                .dataType(DataType.Int8)
                                .defaultValue(Short.valueOf((short) 0))
                                .build()
                ))
                .build();
    }

    private IndexParam.MetricType resolveMetricType() {
        String metricType = milvusProperties.getRag().getVector() == null
                ? null
                : milvusProperties.getRag().getVector().getMetricType();
        if (!StringUtils.hasText(metricType)) {
            return IndexParam.MetricType.COSINE;
        }
        try {
            return IndexParam.MetricType.valueOf(metricType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown Milvus metric type '{}', fallback to COSINE.", metricType);
            return IndexParam.MetricType.COSINE;
        }
    }

    private int resolveDimension() {
        Integer dimension = milvusProperties.getRag().getVector() == null
                ? null
                : milvusProperties.getRag().getVector().getDimension();
        return dimension == null || dimension <= 0 ? 2048 : dimension;
    }
}
