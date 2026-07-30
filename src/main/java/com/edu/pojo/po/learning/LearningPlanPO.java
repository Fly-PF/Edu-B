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
@TableName("edu_learning_plan")
public class LearningPlanPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("case_id")
    private Long caseId;
    @TableField("title")
    private String title;
    @TableField("learning_goal")
    private String learningGoal;
    @TableField("task_steps")
    private String taskSteps;
    @TableField("duration_minutes")
    private Integer durationMinutes;
    @TableField("acceptance_criteria")
    private String acceptanceCriteria;
    @TableField("check_question")
    private String checkQuestion;
    @TableField("expected_signals")
    private String expectedSignals;
    @TableField("teacher_decision")
    private String teacherDecision;
    @TableField("status")
    private String status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
