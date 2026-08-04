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
@TableName("edu_resource_study_record")
public class EduResourceStudyRecordPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("student_id")
    private Long studentId;
    @TableField("assignment_id")
    private Long assignmentId;
    @TableField("course_id")
    private Long courseId;
    @TableField("chapter_id")
    private Long chapterId;
    @TableField("resource_id")
    private Long resourceId;
    private Integer progress;
    @TableField("study_duration")
    private Integer studyDuration;
    @TableField("finish_status")
    private Integer finishStatus;
    @TableField("last_study_time")
    private LocalDateTime lastStudyTime;
    @TableField("create_time")
    private LocalDateTime createTime;
}
