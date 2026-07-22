package com.edu.repository;

import com.edu.common.dto.RagVectorChunkDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagRepository {
    void uploadObject(MultipartFile file, String objectName);

    void insertVectorChunks(List<RagVectorChunkDTO> chunks);
}
