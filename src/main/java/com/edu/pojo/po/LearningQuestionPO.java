package com.edu.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("edu_learning_question")
public class LearningQuestionPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("practice_id")
    private Long practiceId;
    @TableField("question_type")
    private String questionType;
    @TableField("question_content")
    private String questionContent;
    @TableField("options_json")
    private String optionsJson;
    @TableField("reference_answer")
    private String referenceAnswer;
    @TableField("answer_explanation")
    private String answerExplanation;
    @TableField("question_score")
    private Integer questionScore;
    @TableField("sort_order")
    private Integer sortOrder;
}
