package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.gov.GovQuestionSaveRequest;
import com.edu.pojo.vo.gov.GovQuestionAdminVO;
import com.edu.pojo.vo.gov.GovQuestionImportResultVO;

import java.util.List;

public interface GovQuestionAdminService {
    PageResult<GovQuestionAdminVO> pageQuestions(
            String subject,
            String questionType,
            Integer status,
            String keyword,
            Integer pageNum,
            Integer pageSize
    );

    GovQuestionAdminVO getQuestion(Long questionId);

    GovQuestionAdminVO createQuestion(GovQuestionSaveRequest request);

    GovQuestionAdminVO updateQuestion(Long questionId, GovQuestionSaveRequest request);

    void deleteQuestion(Long questionId);

    GovQuestionImportResultVO importQuestions(List<GovQuestionSaveRequest> requests);
}

