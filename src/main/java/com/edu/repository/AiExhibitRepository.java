package com.edu.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.pojo.po.AiPracticeRecordPO;
import com.edu.pojo.po.AiProjectCasePO;

import java.util.List;

public interface AiExhibitRepository {
    IPage<AiProjectCasePO> selectCasePage(
            long pageNum,
            long pageSize,
            String keyword,
            String gradeBand,
            String subjectDirection,
            String practiceType
    );

    List<AiProjectCasePO> selectEnabledCases();

    AiProjectCasePO selectCaseById(Long caseId);

    int insertPracticeRecord(AiPracticeRecordPO record);

    IPage<AiPracticeRecordPO> selectPracticePage(long pageNum, long pageSize, Long userId, Long caseId);
}
