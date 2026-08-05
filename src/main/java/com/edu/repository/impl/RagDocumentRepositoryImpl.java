package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.mapper.RagDocumentMapper;
import com.edu.pojo.po.RagDocumentPO;
import com.edu.repository.RagDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public RagDocumentPO selectKnowledgeBaseDocumentById(Long kbId, Long documentId) {
        return ragDocumentMapper.selectOne(new LambdaQueryWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getId, documentId)
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getDeleted, 0)
                .last("limit 1"));
    }

    @Override
    public RagDocumentPO selectKnowledgeBaseDocument(Long kbId, String fileUrl) {
        return ragDocumentMapper.selectOne(new LambdaQueryWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getFileUrl, fileUrl)
                .eq(RagDocumentPO::getDeleted, 0)
                .last("limit 1"));
    }

    @Override
    public RagDocumentPO selectCourseResourceDocument(Long kbId, Long resourceId) {
        return ragDocumentMapper.selectOne(new LambdaQueryWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getDeleted, 0)
                .apply("JSON_UNQUOTE(JSON_EXTRACT(ext_json, '$.courseResourceId')) = {0}", resourceId)
                .last("limit 1"));
    }

    @Override
    public List<RagDocumentPO> selectKnowledgeBaseDocuments(Long kbId) {
        return ragDocumentMapper.selectList(new LambdaQueryWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getDeleted, 0)
                .orderByDesc(RagDocumentPO::getCreateTime)
                .orderByDesc(RagDocumentPO::getId));
    }

    @Override
    public List<RagDocumentPO> selectDocumentsByIds(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        return ragDocumentMapper.selectList(new LambdaQueryWrapper<RagDocumentPO>()
                .in(RagDocumentPO::getId, documentIds)
                .eq(RagDocumentPO::getDeleted, 0));
    }

    @Override
    public int updateKnowledgeBaseDocument(Long kbId, Long documentId, String docName, String description) {
        return ragDocumentMapper.update(new LambdaUpdateWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getId, documentId)
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getDeleted, 0)
                .set(RagDocumentPO::getDocName, docName)
                .set(RagDocumentPO::getDescription, description)
                .set(RagDocumentPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public int logicalDeleteKnowledgeBaseDocument(Long kbId, Long documentId) {
        return ragDocumentMapper.update(new LambdaUpdateWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getId, documentId)
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getDeleted, 0)
                .set(RagDocumentPO::getDeleted, 1)
                .set(RagDocumentPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public int logicalDeleteKnowledgeBaseDocuments(Long kbId) {
        return ragDocumentMapper.update(new LambdaUpdateWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getDeleted, 0)
                .set(RagDocumentPO::getDeleted, 1)
                .set(RagDocumentPO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public IPage<RagDocumentPO> selectKnowledgeBaseDocumentPage(long pageNum, long pageSize, Long kbId, String docType,
                                                                String docName) {
        LambdaQueryWrapper<RagDocumentPO> queryWrapper = new LambdaQueryWrapper<RagDocumentPO>()
                .eq(RagDocumentPO::getKbId, kbId)
                .eq(RagDocumentPO::getDeleted, 0)
                .eq(StringUtils.hasText(docType), RagDocumentPO::getDocType, normalizeDocType(docType))
                .like(StringUtils.hasText(docName), RagDocumentPO::getDocName, docName == null ? null : docName.trim())
                .orderByDesc(RagDocumentPO::getCreateTime)
                .orderByDesc(RagDocumentPO::getId);
        return ragDocumentMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    private String normalizeDocType(String docType) {
        return docType == null ? null : docType.trim().toLowerCase();
    }
}
