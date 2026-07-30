package com.edu.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("edu_learning_practice")
public class LearningPracticePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("course_id")
    private Long courseId;
    @TableField("practice_title")
    private String practiceTitle;
    @TableField("practice_intro")
    private String practiceIntro;
    @TableField("total_score")
    private Integer totalScore;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
