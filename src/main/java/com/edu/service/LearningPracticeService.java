package com.edu.service;

import com.edu.pojo.dto.practice.PracticeReviewRequest;
import com.edu.pojo.dto.practice.PracticeAiDraftRequest;
import com.edu.pojo.dto.practice.PracticePublishRequest;
import com.edu.pojo.dto.practice.PracticeSubmitRequest;
import com.edu.pojo.vo.practice.PracticeListItemVO;
import com.edu.pojo.vo.practice.StudentPracticeDetailVO;
import com.edu.pojo.vo.practice.TeacherPracticeSubmissionVO;
import com.edu.pojo.vo.practice.TeacherPracticeCourseVO;

import java.util.List;

public interface LearningPracticeService {
    List<PracticeListItemVO> listStudentPractices();

    StudentPracticeDetailVO getStudentPractice(Long practiceId);

    StudentPracticeDetailVO submitPractice(Long practiceId, PracticeSubmitRequest request);

    List<TeacherPracticeSubmissionVO> listTeacherSubmissions(String status);

    TeacherPracticeSubmissionVO getTeacherSubmission(Long submissionId);

    TeacherPracticeSubmissionVO saveAiReviewDraft(Long submissionId, PracticeAiDraftRequest request);

    TeacherPracticeSubmissionVO reviewSubmission(Long submissionId, PracticeReviewRequest request);

    List<TeacherPracticeCourseVO> listTeacherPracticeCourses();

    Long publishPractice(PracticePublishRequest request);

    void deletePractice(Long practiceId);
}
