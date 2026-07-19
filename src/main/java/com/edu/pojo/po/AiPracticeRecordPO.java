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
@TableName("ai_practice_record")
public class AiPracticeRecordPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("case_id")
    private Long caseId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("practice_type")
    private String practiceType;

    @TableField("input_text")
    private String inputText;

    @TableField("file_url")
    private String fileUrl;

    @TableField("file_name")
    private String fileName;

    @TableField("answer_text")
    private String answerText;

    @TableField("note")
    private String note;

    @TableField("ai_result_json")
    private String aiResultJson;

    @TableField("score")
    private Integer score;

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
