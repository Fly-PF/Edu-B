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
@TableName("ai_project_case")
public class AiProjectCasePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("project_code")
    private String projectCode;

    @TableField("project_name")
    private String projectName;

    @TableField("case_summary")
    private String caseSummary;

    @TableField("grade_band")
    private String gradeBand;

    @TableField("subject_direction")
    private String subjectDirection;

    @TableField("project_background")
    private String projectBackground;

    @TableField("learning_goals_json")
    private String learningGoalsJson;

    @TableField("ai_capability")
    private String aiCapability;

    @TableField("practice_type")
    private String practiceType;

    @TableField("task_steps_json")
    private String taskStepsJson;

    @TableField("required_tools_json")
    private String requiredToolsJson;

    @TableField("example_code")
    private String exampleCode;

    @TableField("submission_requirements")
    private String submissionRequirements;

    @TableField("evaluation_rubric_json")
    private String evaluationRubricJson;

    @TableField("cover")
    private String cover;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("challenge_level")
    private Integer challengeLevel;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("deleted")
    private Integer deleted;

    @TableField("ext_json")
    private String extJson;
}
