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
@TableName(value = "edu_chapter")
public class EduChapterPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "course_id")
    private Long courseId;

    @TableField(value = "chapter_name")
    private String chapterName;

    @TableField(value = "sort")
    private Integer sort;

    @TableField(value = "duration")
    private Integer duration;

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
