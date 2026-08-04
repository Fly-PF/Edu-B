package com.edu.pojo.vo.course;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChapterResourceProgressVO {
    private Long chapterId;
    private Integer progress;
    private Integer finishStatus;
    private Boolean hasResourceRecords;
}
