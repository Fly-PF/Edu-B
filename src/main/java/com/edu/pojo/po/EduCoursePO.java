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
@TableName(value = "edu_course")
public class EduCoursePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "course_name")
    private String courseName;

    @TableField(value = "cover")
    private String cover;

    @TableField(value = "grade")
    private String grade;

    @TableField(value = "difficulty")
    private Integer difficulty;

    @TableField(value = "course_type")
    private Integer courseType;

    @TableField(value = "teacher_id")
    private Long teacherId;

    @TableField(value = "intro")
    private String intro;

    @TableField(value = "total_duration")
    private Integer totalDuration;

    @TableField(value = "total_chapter")
    private Integer totalChapter;

    @TableField(value = "series_name")
    private String seriesName;

    @TableField(value = "series_order")
    private Integer seriesOrder;

    @TableField(value = "like_count")
    private Integer likeCount;

    @TableField(value = "publish_time")
    private LocalDateTime publishTime;

    @TableField(value = "is_public")
    private Integer publicFlag;

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
