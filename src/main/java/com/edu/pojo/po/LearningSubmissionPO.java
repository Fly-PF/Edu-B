package com.edu.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("edu_learning_submission")
public class LearningSubmissionPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("practice_id")
    private Long practiceId;
    @TableField("student_id")
    private Long studentId;
    @TableField("student_name")
    private String studentName;
    @TableField("answer_json")
    private String answerJson;
    @TableField("question_review_json")
    private String questionReviewJson;
    @TableField("auto_score")
    private Integer autoScore;
    @TableField("teacher_score")
    private Integer teacherScore;
    @TableField("teacher_feedback")
    private String teacherFeedback;
    private String status;
    @TableField("submit_time")
    private LocalDateTime submitTime;
    @TableField("review_time")
    private LocalDateTime reviewTime;
    @TableField("reviewer_id")
    private Long reviewerId;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
