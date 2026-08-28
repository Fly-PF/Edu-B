package com.edu.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("edu_gov_user_goal")
public class EduGovUserGoalPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id") private Long userId;
    @TableField("exam_type") private String examType;
    @TableField("exam_name") private String examName;
    @TableField("exam_date") private LocalDate examDate;
    private String note;
    @TableField("create_time") private LocalDateTime createTime;
    @TableField("update_time") private LocalDateTime updateTime;
    private Integer deleted;
}
