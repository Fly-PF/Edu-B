package com.edu.pojo.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("edu_gov_plan_task")
public class EduGovPlanTaskPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id") private Long userId;
    @TableField("task_date") private LocalDate taskDate;
    private String title;
    @TableField("task_type") private String taskType;
    private Integer status;
    @TableField("completed_at") private LocalDateTime completedAt;
    @TableField("create_time") private LocalDateTime createTime;
    @TableField("update_time") private LocalDateTime updateTime;
    private Integer deleted;
}
