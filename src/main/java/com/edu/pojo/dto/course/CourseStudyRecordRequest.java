package com.edu.pojo.dto.course;

import lombok.Data;

@Data
public class CourseStudyRecordRequest {
    private Long chapterId;
    private Long resourceId;
    private Integer progress;
    private Integer studyDuration;
    private Integer finishStatus;
}
