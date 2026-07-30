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
@TableName("edu_learning_case")
public class LearningCasePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("class_id")
    private Long classId;

    @TableField("course_id")
    private Long courseId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField("student_id")
    private Long studentId;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("risk_score")
    private Integer riskScore;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("behavior_snapshot")
    private String behaviorSnapshot;

    @TableField("diagnosis")
    private String diagnosis;

    @TableField("diagnosis_source")
    private String diagnosisSource;

    @TableField("model_name")
    private String modelName;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
