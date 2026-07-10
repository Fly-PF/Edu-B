package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseStudyRecordDTO {
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long courseId;
    private String courseName;
    private Integer totalChapter;
    private Integer finishedChapter;
    private Integer progress;
    private Integer studyDuration;
    private Integer studyStatus;
    private String lastStudyTime;
}
