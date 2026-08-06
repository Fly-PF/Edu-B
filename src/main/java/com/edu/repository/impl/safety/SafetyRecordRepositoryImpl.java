package com.edu.repository.impl.safety;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.PageQuery;
import com.edu.mapper.safety.SafetyRecordMapper;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyReviewStatus;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.pojo.po.safety.SafetyRecordPO;
import com.edu.repository.safety.SafetyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SafetyRecordRepositoryImpl implements SafetyRecordRepository {
    private final SafetyRecordMapper safetyRecordMapper;

    @Override
    public int insert(SafetyRecordPO record) {
        return safetyRecordMapper.insert(record);
    }

    @Override
    public SafetyRecordPO selectById(Long id) {
        if (id == null) {
            return null;
        }
        return safetyRecordMapper.selectById(id);
    }

    @Override
    public IPage<SafetyRecordPO> pageRecords(PageQuery pageQuery,
                                             SafetySourceModule sourceModule,
                                             SafetyScene scene,
                                             SafetyUserRole userRole,
                                             SafetyGradeLevel gradeLevel,
                                             SafetyRiskLevel riskLevel,
                                             SafetyRiskType riskType,
                                             SafetyDecision decision,
                                             SafetyReviewStatus reviewStatus,
                                             Boolean manualReviewRequired,
                                             String keyword) {
        LambdaQueryWrapper<SafetyRecordPO> queryWrapper = baseWrapper()
                .eq(sourceModule != null, SafetyRecordPO::getSourceModule, name(sourceModule))
                .eq(scene != null, SafetyRecordPO::getScene, name(scene))
                .eq(userRole != null, SafetyRecordPO::getUserRole, name(userRole))
                .eq(gradeLevel != null, SafetyRecordPO::getGradeLevel, name(gradeLevel))
                .eq(riskLevel != null, SafetyRecordPO::getRiskLevel, name(riskLevel))
                .like(riskType != null, SafetyRecordPO::getRiskTypes, name(riskType))
                .eq(decision != null, SafetyRecordPO::getDecision, name(decision))
                .and(reviewStatus == SafetyReviewStatus.PENDING, wrapper -> wrapper
                        .nested(nested -> nested
                                .eq(SafetyRecordPO::getReviewStatus, SafetyReviewStatus.PENDING.name())
                                .or(or -> or
                                        .isNull(SafetyRecordPO::getReviewStatus)
                                        .eq(SafetyRecordPO::getManualReviewRequired, true))))
                .eq(reviewStatus != null && reviewStatus != SafetyReviewStatus.PENDING, SafetyRecordPO::getReviewStatus, name(reviewStatus))
                .eq(manualReviewRequired != null, SafetyRecordPO::getManualReviewRequired, manualReviewRequired)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SafetyRecordPO::getInputText, keyword)
                        .or()
                        .like(SafetyRecordPO::getOutputText, keyword)
                        .or()
                        .like(SafetyRecordPO::getReason, keyword)
                        .or()
                        .like(SafetyRecordPO::getSuggestion, keyword));
        return safetyRecordMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), queryWrapper);
    }

    @Override
    public IPage<SafetyRecordPO> pageReviewRecords(PageQuery pageQuery,
                                                   Long classId,
                                                   SafetySourceModule sourceModule,
                                                   SafetyScene scene,
                                                   SafetyUserRole userRole,
                                                   SafetyGradeLevel gradeLevel,
                                                   SafetyRiskLevel riskLevel,
                                                   SafetyRiskType riskType,
                                                   SafetyDecision decision,
                                                   SafetyReviewStatus reviewStatus,
                                                   Boolean manualReviewRequired,
                                                   String keyword) {
        LambdaQueryWrapper<SafetyRecordPO> queryWrapper = baseWrapper()
                .eq(classId != null, SafetyRecordPO::getClassId, classId)
                .eq(sourceModule != null, SafetyRecordPO::getSourceModule, name(sourceModule))
                .eq(scene != null, SafetyRecordPO::getScene, name(scene))
                .eq(userRole != null, SafetyRecordPO::getUserRole, name(userRole))
                .eq(gradeLevel != null, SafetyRecordPO::getGradeLevel, name(gradeLevel))
                .eq(riskLevel != null, SafetyRecordPO::getRiskLevel, name(riskLevel))
                .like(riskType != null, SafetyRecordPO::getRiskTypes, name(riskType))
                .eq(decision != null, SafetyRecordPO::getDecision, name(decision))
                .eq(manualReviewRequired != null, SafetyRecordPO::getManualReviewRequired, manualReviewRequired)
                .and(reviewStatus == SafetyReviewStatus.PENDING, wrapper -> wrapper
                        .nested(nested -> nested
                                .eq(SafetyRecordPO::getReviewStatus, SafetyReviewStatus.PENDING.name())
                                .or(or -> or
                                        .isNull(SafetyRecordPO::getReviewStatus)
                                        .eq(SafetyRecordPO::getManualReviewRequired, true))))
                .eq(reviewStatus != null && reviewStatus != SafetyReviewStatus.PENDING, SafetyRecordPO::getReviewStatus, name(reviewStatus))
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SafetyRecordPO::getInputText, keyword)
                        .or()
                        .like(SafetyRecordPO::getOutputText, keyword)
                        .or()
                        .like(SafetyRecordPO::getReason, keyword)
                        .or()
                        .like(SafetyRecordPO::getSuggestion, keyword));
        return safetyRecordMapper.selectPage(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()), queryWrapper);
    }

    @Override
    public int updateReview(Long id,
                            SafetyReviewStatus reviewStatus,
                            Long reviewBy,
                            String reviewByName,
                            String reviewComment,
                            LocalDateTime reviewTime,
                            Boolean manualReviewRequired) {
        LambdaUpdateWrapper<SafetyRecordPO> updateWrapper = new LambdaUpdateWrapper<SafetyRecordPO>()
                .eq(SafetyRecordPO::getId, id)
                .set(reviewStatus != null, SafetyRecordPO::getReviewStatus, name(reviewStatus))
                .set(SafetyRecordPO::getReviewBy, reviewBy)
                .set(SafetyRecordPO::getReviewByName, reviewByName)
                .set(SafetyRecordPO::getReviewComment, reviewComment)
                .set(SafetyRecordPO::getReviewTime, reviewTime)
                .set(manualReviewRequired != null, SafetyRecordPO::getManualReviewRequired, manualReviewRequired);
        return safetyRecordMapper.update(null, updateWrapper);
    }

    @Override
    public List<SafetyRecordPO> selectSince(LocalDateTime startTime) {
        LambdaQueryWrapper<SafetyRecordPO> queryWrapper = baseWrapper()
                .ge(startTime != null, SafetyRecordPO::getCreateTime, startTime);
        return safetyRecordMapper.selectList(queryWrapper);
    }

    @Override
    public List<SafetyRecordPO> selectRecentHighRisk(int limit) {
        LambdaQueryWrapper<SafetyRecordPO> queryWrapper = baseWrapper()
                .eq(SafetyRecordPO::getRiskLevel, SafetyRiskLevel.HIGH.name())
                .last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        return safetyRecordMapper.selectList(queryWrapper);
    }

    private LambdaQueryWrapper<SafetyRecordPO> baseWrapper() {
        return new LambdaQueryWrapper<SafetyRecordPO>()
                .orderByDesc(SafetyRecordPO::getCreateTime)
                .orderByDesc(SafetyRecordPO::getId);
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
