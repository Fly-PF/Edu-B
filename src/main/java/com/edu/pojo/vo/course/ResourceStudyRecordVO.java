package com.edu.pojo.vo.course;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResourceStudyRecordVO {
    private Long resourceId;
    private Long assignmentId;
    private Long chapterId;
    private Integer progress;
    private Integer studyDuration;
    private Integer finishStatus;
    private LocalDateTime lastStudyTime;
}
