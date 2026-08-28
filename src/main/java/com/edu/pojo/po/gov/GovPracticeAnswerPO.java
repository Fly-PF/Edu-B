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
@TableName("edu_gov_practice_answer")
public class GovPracticeAnswerPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("practice_id")
    private Long practiceId;

    @TableField("question_id")
    private Long questionId;

    @TableField("question_order")
    private Integer questionOrder;

    @TableField("selected_answer_json")
    private String selectedAnswerJson;

    @TableField("is_correct")
    private Integer isCorrect;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    private Integer deleted;
}

