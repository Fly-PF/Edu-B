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
    private Integer sortOrder;
    private Integer duration;
    private LocalDateTime createdTime;
    private List<ResourceVO> resources;
}
