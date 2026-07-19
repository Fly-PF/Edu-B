package com.edu.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.mapper.AiPracticeRecordMapper;
import com.edu.mapper.AiProjectCaseMapper;
import com.edu.pojo.po.AiPracticeRecordPO;
import com.edu.pojo.po.AiProjectCasePO;
import com.edu.repository.AiExhibitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiExhibitRepositoryImpl implements AiExhibitRepository {
    private static final int ENABLED = 1;
    private static final int NOT_DELETED = 0;

    private final AiProjectCaseMapper caseMapper;
    private final AiPracticeRecordMapper practiceRecordMapper;

    @Override
    public IPage<AiProjectCasePO> selectCasePage(
            long pageNum,
            long pageSize,
            String keyword,
            String gradeBand,
            String subjectDirection,
            String practiceType
    ) {
        LambdaQueryWrapper<AiProjectCasePO> queryWrapper = caseQuery()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(AiProjectCasePO::getProjectName, keyword)
                        .or()
                        .like(AiProjectCasePO::getCaseSummary, keyword)
                        .or()
                        .like(AiProjectCasePO::getSubjectDirection, keyword)
                        .or()
                        .like(AiProjectCasePO::getAiCapability, keyword))
                .eq(StringUtils.hasText(gradeBand), AiProjectCasePO::getGradeBand, gradeBand)
                .eq(StringUtils.hasText(subjectDirection), AiProjectCasePO::getSubjectDirection, subjectDirection)
                .eq(StringUtils.hasText(practiceType), AiProjectCasePO::getPracticeType, practiceType)
                .orderByAsc(AiProjectCasePO::getSort)
                .orderByDesc(AiProjectCasePO::getCreateTime)
                .orderByDesc(AiProjectCasePO::getId);
        return caseMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    @Override
    public List<AiProjectCasePO> selectEnabledCases() {
        return caseMapper.selectList(caseQuery()
                .orderByAsc(AiProjectCasePO::getSort)
                .orderByAsc(AiProjectCasePO::getId));
    }

    @Override
    public AiProjectCasePO selectCaseById(Long caseId) {
        if (caseId == null) {
            return null;
        }
        return caseMapper.selectOne(caseQuery().eq(AiProjectCasePO::getId, caseId));
    }

    @Override
    public int insertPracticeRecord(AiPracticeRecordPO record) {
        return practiceRecordMapper.insert(record);
    }

    @Override
    public IPage<AiPracticeRecordPO> selectPracticePage(long pageNum, long pageSize, Long userId, Long caseId) {
        LambdaQueryWrapper<AiPracticeRecordPO> queryWrapper = new LambdaQueryWrapper<AiPracticeRecordPO>()
                .eq(AiPracticeRecordPO::getDeleted, NOT_DELETED)
                .eq(userId != null, AiPracticeRecordPO::getUserId, userId)
                .eq(caseId != null, AiPracticeRecordPO::getCaseId, caseId)
                .orderByDesc(AiPracticeRecordPO::getCreateTime)
                .orderByDesc(AiPracticeRecordPO::getId);
        return practiceRecordMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    private LambdaQueryWrapper<AiProjectCasePO> caseQuery() {
        return new LambdaQueryWrapper<AiProjectCasePO>()
                .eq(AiProjectCasePO::getDeleted, NOT_DELETED)
                .eq(AiProjectCasePO::getStatus, ENABLED);
    }
}
