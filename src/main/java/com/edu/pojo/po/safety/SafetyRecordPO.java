package com.edu.pojo.po.safety;

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
@TableName("edu_safety_record")
public class SafetyRecordPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("source_module")
    private String sourceModule;

    @TableField("scene")
    private String scene;

    @TableField("user_role")
    private String userRole;

    @TableField("grade_level")
    private String gradeLevel;

    @TableField("user_id")
    private Long userId;

    @TableField("class_id")
    private Long classId;

    @TableField("course_id")
    private Long courseId;

    @TableField("chapter_id")
    private Long chapterId;

    @TableField("input_text")
    private String inputText;

    @TableField("output_text")
    private String outputText;

    @TableField("allowed")
    private Boolean allowed;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("risk_types")
    private String riskTypes;

    @TableField("decision")
    private String decision;

    @TableField("reason")
    private String reason;

    @TableField("suggestion")
    private String suggestion;

    @TableField("processed_text")
    private String processedText;

    @TableField("evidence_level")
    private String evidenceLevel;

    @TableField("evidence_score")
    private Double evidenceScore;

    @TableField("manual_review_required")
    private Boolean manualReviewRequired;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_by")
    private Long reviewBy;

    @TableField("review_by_name")
    private String reviewByName;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("metadata_json")
    private String metadataJson;

    @TableField("debug_json")
    private String debugJson;

    @TableField("create_time")
    private LocalDateTime createTime;
}
