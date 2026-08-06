package com.edu.repository.safety;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.common.PageQuery;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyReviewStatus;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.pojo.po.safety.SafetyRecordPO;

import java.time.LocalDateTime;
import java.util.List;

public interface SafetyRecordRepository {
    int insert(SafetyRecordPO record);

    SafetyRecordPO selectById(Long id);

    IPage<SafetyRecordPO> pageRecords(PageQuery pageQuery,
                                      SafetySourceModule sourceModule,
                                      SafetyScene scene,
                                      SafetyUserRole userRole,
                                      SafetyGradeLevel gradeLevel,
                                     SafetyRiskLevel riskLevel,
                                     SafetyRiskType riskType,
                                     SafetyDecision decision,
                                     SafetyReviewStatus reviewStatus,
                                     Boolean manualReviewRequired,
                                     String keyword);

    IPage<SafetyRecordPO> pageReviewRecords(PageQuery pageQuery,
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
                                            String keyword);

    int updateReview(Long id,
                     SafetyReviewStatus reviewStatus,
                     Long reviewBy,
                     String reviewByName,
                     String reviewComment,
                     LocalDateTime reviewTime,
                     Boolean manualReviewRequired);

    List<SafetyRecordPO> selectSince(LocalDateTime startTime);

    List<SafetyRecordPO> selectRecentHighRisk(int limit);
}
