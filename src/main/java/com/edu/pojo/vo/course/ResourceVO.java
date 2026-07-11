package com.edu.pojo.vo.course;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResourceVO {
    private Long id;
    private Long chapterId;
    private String name;
    private Integer type;
    private String url;
    private String storedUrl;
    private Long fileSize;
    private Integer duration;
    private Integer sortOrder;
    private LocalDateTime createdTime;
}
