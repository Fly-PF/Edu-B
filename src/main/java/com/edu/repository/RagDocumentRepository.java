package com.edu.repository;

import com.edu.pojo.po.RagDocumentPO;

public interface RagDocumentRepository {
    int insertDocument(RagDocumentPO document);

    int deleteDocumentById(Long documentId);
}
