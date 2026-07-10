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
@TableName(value = "edu_class_student")
public class EduClassStudentPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "class_id")
    private Long classId;

    @TableField(value = "student_id")
    private Long studentId;

    @TableField(value = "join_time")
    private LocalDateTime joinTime;
}
