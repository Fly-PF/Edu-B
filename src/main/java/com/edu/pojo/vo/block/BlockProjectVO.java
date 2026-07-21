package com.edu.pojo.vo.block;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BlockProjectVO {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String title;
    private String description;
    private String workspaceJson;
    private String stageJson;
    private String thumbnailData;
    private Boolean published;
    private Long sourceProjectId;
    private Integer remixCount;
    private Integer viewCount;
    private LocalDateTime publishedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
