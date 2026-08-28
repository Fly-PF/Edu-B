package com.edu.service;

import com.edu.pojo.dto.gov.GovMockExamCreateRequest;
import com.edu.pojo.dto.gov.GovMockExamSubmitRequest;
import com.edu.pojo.vo.gov.GovMockExamRecordVO;
import com.edu.pojo.vo.gov.GovMockExamReportVO;
import com.edu.pojo.vo.gov.GovMockExamVO;

import java.util.List;

public interface GovAssessmentService {
    GovMockExamVO createMockExam(GovMockExamCreateRequest request);

    GovMockExamVO getMockExam(Long practiceId);

    GovMockExamReportVO submitMockExam(Long practiceId, GovMockExamSubmitRequest request);

    GovMockExamReportVO getMockExamReport(Long practiceId);

    List<GovMockExamRecordVO> listMockExamRecords();
}

