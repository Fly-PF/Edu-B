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
@TableName("edu_ai_skill_call_log")
public class AiSkillCallLogPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("skill_id")
    private Long skillId;

    @TableField("user_id")
    private Long userId;

    @TableField("source_module")
    private String sourceModule;

    @TableField("input_text")
    private String inputText;

    @TableField("output_text")
    private String outputText;

    @TableField("success")
    private Integer success;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private LocalDateTime createTime;
}
