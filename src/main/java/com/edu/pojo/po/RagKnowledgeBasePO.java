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
@TableName(value = "rag_knowledge_base")
public class RagKnowledgeBasePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "kb_name")
    private String kbName;

    @TableField(value = "kb_cover")
    private String kbCover;

    @TableField(value = "description")
    private String description;

    @TableField(value = "kb_type")
    private Integer kbType;

    @TableField(value = "is_public")
    private Integer publicFlag;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "course_id")
    private Long courseId;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
