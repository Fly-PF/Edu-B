package com.edu.repository.impl;

import com.edu.mapper.RagDocumentMapper;
import com.edu.pojo.po.RagDocumentPO;
import com.edu.repository.RagDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RagDocumentRepositoryImpl implements RagDocumentRepository {
    private final RagDocumentMapper ragDocumentMapper;

    @Override
    public int insertDocument(RagDocumentPO document) {
        return ragDocumentMapper.insert(document);
    }

    @Override
    public int deleteDocumentById(Long documentId) {
        return ragDocumentMapper.deleteById(documentId);
    }
}
