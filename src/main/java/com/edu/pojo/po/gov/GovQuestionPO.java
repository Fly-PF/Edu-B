package com.edu.pojo.po.gov;

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
@TableName("edu_gov_question")
public class GovQuestionPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String subject;

    @TableField("question_type")
    private String questionType;

    private Integer difficulty;

    @TableField("exam_year")
    private Integer examYear;

    @TableField("source_type")
    private String sourceType;

    @TableField("content_json")
    private String contentJson;

    private Integer status;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    private Integer deleted;
}

