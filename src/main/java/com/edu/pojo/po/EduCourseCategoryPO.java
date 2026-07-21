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
@TableName("edu_course_category")
public class EduCourseCategoryPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("match_all")
    private Integer matchAll;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    private Integer deleted;
}
