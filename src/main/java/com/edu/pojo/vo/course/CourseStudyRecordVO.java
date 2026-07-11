package com.edu.pojo.vo.course;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseStudyRecordVO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long chapterId;
    private Long resourceId;
    private Integer progress;
    private Integer studyDuration;
    private Integer finishStatus;
    private LocalDateTime lastStudyTime;
    private LocalDateTime createdTime;
}
