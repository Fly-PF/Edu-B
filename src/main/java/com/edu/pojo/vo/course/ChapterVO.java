package com.edu.pojo.vo.course;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChapterVO {
    private Long id;
    private Long courseId;
    private String title;
    private String chapterName;
    private Integer sortOrder;
    private Integer sort;
    private Integer duration;
    private Integer progress;
    private Integer finishStatus;
    private LocalDateTime createdTime;
    private List<ResourceVO> resources;
}
