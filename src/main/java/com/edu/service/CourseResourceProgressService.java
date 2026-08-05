package com.edu.service;

import com.edu.pojo.dto.course.ResourceStudyRecordRequest;
import com.edu.pojo.vo.course.ResourceStudyRecordVO;
import com.edu.pojo.vo.course.ChapterResourceProgressVO;

import java.util.List;

public interface CourseResourceProgressService {
    List<ResourceStudyRecordVO> listRecords(Long courseId, Long assignmentId);

    ResourceStudyRecordVO save(ResourceStudyRecordRequest request);

    ResourceStudyRecordVO openBlockProject(ResourceStudyRecordRequest request);

    ResourceStudyRecordVO completeBlockProject(ResourceStudyRecordRequest request);

    List<ChapterResourceProgressVO> summarizeChapters(Long studentId, Long courseId, Long assignmentId);
}
