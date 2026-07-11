package com.edu.repository;

import com.edu.pojo.po.EduStudyRecordPO;

import java.util.List;

public interface EduStudyRecordRepository {
    EduStudyRecordPO selectStudyRecord(Long studentId, Long chapterId);

    List<EduStudyRecordPO> selectRecordsByStudentId(Long studentId);

    List<EduStudyRecordPO> selectRecordsByCourseId(Long courseId);

    int insertStudyRecord(EduStudyRecordPO record);

    int updateStudyRecord(EduStudyRecordPO record);
}
