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
@TableName("edu_learning_wrong_book_item")
public class LearningWrongBookItemPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("book_id")
    private Long bookId;

    @TableField("student_id")
    private Long studentId;

    @TableField("practice_id")
    private Long practiceId;

    @TableField("question_id")
    private Long questionId;

    @TableField("practice_title")
    private String practiceTitle;

    @TableField("course_name")
    private String courseName;

    @TableField("question_content")
    private String questionContent;

    @TableField("question_score")
    private Integer questionScore;

    @TableField("awarded_score")
    private Integer awardedScore;

    @TableField("reference_answer")
    private String referenceAnswer;

    private String explanation;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
