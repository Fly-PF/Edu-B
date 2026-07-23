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
@TableName(value = "edu_ai_companion_message")
public class AiCompanionMessagePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    private Long sessionId;

    @TableField(value = "student_id")
    private Long studentId;

    @TableField(value = "role")
    private String role;

    @TableField(value = "content")
    private String content;

    @TableField(value = "chapter_id")
    private Long chapterId;

    @TableField(value = "resource_id")
    private Long resourceId;

    @TableField(value = "generation_mode")
    private String generationMode;

    @TableField(value = "model_name")
    private String modelName;

    @TableField(value = "source_summary")
    private String sourceSummary;

    @TableField(value = "safety_status")
    private String safetyStatus;

    @TableField(value = "response_time_ms")
    private Long responseTimeMs;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
