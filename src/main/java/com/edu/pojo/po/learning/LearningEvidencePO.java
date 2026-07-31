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
@TableName("edu_learning_evidence")
public class LearningEvidencePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("plan_id")
    private Long planId;
    @TableField("student_id")
    private Long studentId;
    @TableField("reflection")
    private String reflection;
    @TableField("difficulty")
    private String difficulty;
    @TableField("answer")
    private String answer;
    @TableField("ai_assessment")
    private String aiAssessment;
    @TableField("confidence")
    private Integer confidence;
    @TableField("result")
    private String result;
    @TableField("assessment_source")
    private String assessmentSource;
    @TableField("teacher_conclusion")
    private String teacherConclusion;
    @TableField("submitted_at")
    private LocalDateTime submittedAt;
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;
}
