package com.edu.pojo.po.skill;

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
@TableName("edu_ai_skill_category")
public class AiSkillCategoryPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("category_type")
    private String categoryType;

    @TableField("category_name")
    private String categoryName;

    private Integer sort;

    private Integer enabled;

    private Integer deleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
