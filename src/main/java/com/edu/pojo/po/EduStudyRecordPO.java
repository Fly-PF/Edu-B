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
@TableName(value = "edu_study_record")
public class EduStudyRecordPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "student_id")
    private Long studentId;

    @TableField(value = "course_id")
    private Long courseId;

    @TableField(value = "chapter_id")
    private Long chapterId;

    @TableField(value = "resource_id")
    private Long resourceId;

    @TableField(value = "progress")
    private Integer progress;

    @TableField(value = "study_duration")
    private Integer studyDuration;

    @TableField(value = "finish_status")
    private Integer finishStatus;

    @TableField(value = "last_study_time")
    private LocalDateTime lastStudyTime;

    @TableField(value = "create_time")
    private LocalDateTime createTime;
}
