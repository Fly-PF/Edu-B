package com.edu.pojo.dto.course;

import lombok.Data;

@Data
public class ResourceStudyRecordRequest {
    private Long assignmentId;
    private Long courseId;
    private Long chapterId;
    private Long resourceId;
    private Integer progress;
    private Integer studyDuration;
    private Integer finishStatus;
}
