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
    private String resourceName;
    private Integer type;
    private Integer resourceType;
    private String url;
    private String resourceUrl;
    private String storedUrl;
    private Long fileSize;
    private Integer duration;
    private Integer sortOrder;
    private LocalDateTime createdTime;
}
