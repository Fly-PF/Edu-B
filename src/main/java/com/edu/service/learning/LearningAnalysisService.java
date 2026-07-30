package com.edu.service.learning;

import com.edu.pojo.dto.learning.LearningCaseGenerateRequest;
import com.edu.pojo.dto.learning.LearningAssistantRequest;
import com.edu.pojo.dto.learning.LearningEvidenceSubmitRequest;
import com.edu.pojo.dto.learning.LearningPlanDecisionRequest;
import com.edu.pojo.dto.learning.LearningPlanReviewRequest;
import com.edu.pojo.vo.learning.LearningGrowthCaseVO;
import com.edu.pojo.vo.learning.LearningAssistantReplyVO;
import com.edu.pojo.vo.learning.LearningStudentGrowthVO;
import com.edu.pojo.vo.learning.LearningTeacherGrowthVO;

public interface LearningAnalysisService {
    LearningTeacherGrowthVO getTeacherDashboard(Long classId);

    LearningGrowthCaseVO generateCase(LearningCaseGenerateRequest request);

    LearningGrowthCaseVO decidePlan(Long caseId, LearningPlanDecisionRequest request);

    LearningGrowthCaseVO reviewPlan(Long planId, LearningPlanReviewRequest request);

    LearningStudentGrowthVO getStudentGrowthOverview();

    LearningStudentGrowthVO refreshStudentCourseRecommendations();

    LearningAssistantReplyVO askStudentAssistant(LearningAssistantRequest request);

    LearningAssistantReplyVO askTeacherAssistant(Long classId, LearningAssistantRequest request);

    LearningGrowthCaseVO submitEvidence(Long planId, LearningEvidenceSubmitRequest request);
}
