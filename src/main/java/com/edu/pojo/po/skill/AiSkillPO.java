package com.edu.pojo.po.skill;

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
@TableName("edu_ai_skill")
public class AiSkillPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("skill_name")
    private String skillName;

    private String description;

    @TableField("skill_type")
    private String skillType;

    @TableField("subject_type")
    private String subjectType;

    @TableField("target_audience")
    private String targetAudience;

    @TableField("usage_guide")
    private String usageGuide;

    @TableField("example_input")
    private String exampleInput;

    @TableField("example_output")
    private String exampleOutput;

    private String visibility;

    private String status;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("review_message")
    private String reviewMessage;

    @TableField("root_object_path")
    private String rootObjectPath;

    private Integer deleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
