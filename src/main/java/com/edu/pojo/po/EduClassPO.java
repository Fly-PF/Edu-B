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
@TableName(value = "edu_class")
public class EduClassPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "class_name")
    private String className;

    @TableField(value = "teacher_id")
    private Long teacherId;

    @TableField(value = "grade")
    private String grade;

    @TableField(value = "school")
    private String school;

    @TableField(value = "class_code")
    private String classCode;

    @TableField(value = "join_type")
    private Integer joinType;

    @TableField(value = "student_count")
    private Integer studentCount;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "create_by")
    private Long createBy;

    @TableField(value = "update_by")
    private Long updateBy;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(value = "deleted")
    private Integer deleted;

    @TableField(value = "ext_json")
    private String extJson;
}
