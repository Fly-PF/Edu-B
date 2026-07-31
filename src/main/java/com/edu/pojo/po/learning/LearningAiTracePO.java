package com.edu.pojo.po.learning;

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
@TableName("edu_learning_ai_trace")
public class LearningAiTracePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("case_id")
    private Long caseId;
    @TableField("plan_id")
    private Long planId;
    @TableField("student_id")
    private Long studentId;
    @TableField("operation")
    private String operation;
    @TableField("model_name")
    private String modelName;
    @TableField("source")
    private String source;
    @TableField("context_summary")
    private String contextSummary;
    @TableField("elapsed_millis")
    private Long elapsedMillis;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
