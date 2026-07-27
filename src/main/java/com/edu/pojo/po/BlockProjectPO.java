package com.edu.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("block_project")
public class BlockProjectPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("owner_id")
    private Long ownerId;
    @TableField("owner_name")
    private String ownerName;
    private String title;
    private String description;
    @TableField("workspace_json")
    private String workspaceJson;
    @TableField("stage_json")
    private String stageJson;
    @TableField("thumbnail_data")
    private String thumbnailData;
    private Integer visibility;
    @TableField("source_project_id")
    private Long sourceProjectId;
    @TableField("remix_count")
    private Integer remixCount;
    @TableField("view_count")
    private Integer viewCount;
    @TableField("published_time")
    private LocalDateTime publishedTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    private Integer deleted;
}
