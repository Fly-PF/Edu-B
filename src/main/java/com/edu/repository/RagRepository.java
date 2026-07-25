package com.edu.repository;

import com.edu.common.dto.RagVectorChunkDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagRepository {
    void uploadObject(MultipartFile file, String objectName);

    void deleteObject(String objectName);

    void deleteObjectStrict(String objectName);

    void insertVectorChunks(List<RagVectorChunkDTO> chunks);

    void deleteVectorChunks(Long kbId, Long docId);

    void logicalDeleteVectorChunks(Long kbId, Long docId);

    List<RagSearchChunk> searchVectorChunks(float[] vector, List<Long> kbIds);

    record RagSearchChunk(Long kbId, Long docId, String sourceInfo, String content, Float score) {
    }
}
