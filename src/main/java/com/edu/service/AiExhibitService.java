package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.vo.ai.AiExhibitOverviewVO;
import com.edu.pojo.vo.ai.AiPracticeRecordVO;
import com.edu.pojo.vo.ai.AiProjectCaseVO;
import org.springframework.web.multipart.MultipartFile;

public interface AiExhibitService {
    AiExhibitOverviewVO getOverview();

    PageResult<AiProjectCaseVO> listCases(
            Integer pageNum,
            Integer pageSize,
            String keyword,
            String gradeBand,
            String subjectDirection,
            String practiceType
    );

    AiProjectCaseVO getCase(Long caseId);

    PageResult<AiPracticeRecordVO> listMyRecords(Integer pageNum, Integer pageSize, Long caseId);

    AiPracticeRecordVO submitPractice(
            Long caseId,
            String practiceType,
            String inputText,
            String answerText,
            String note,
            MultipartFile file
    );
}
