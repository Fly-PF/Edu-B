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
@TableName("edu_learning_recommendation")
public class LearningRecommendationPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("batch_id")
    private String batchId;

    @TableField("student_id")
    private Long studentId;

    @TableField("course_id")
    private Long courseId;

    @TableField("recommendation_score")
    private Integer recommendationScore;

    private String reason;

    private String source;

    @TableField("model_name")
    private String modelName;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
