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
@TableName("edu_gov_news")
public class EduGovNewsPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("category_id")
    private Long categoryId;
    @TableField("title")
    private String title;
    @TableField("summary")
    private String summary;
    @TableField("content_md")
    private String contentMd;
    @TableField("cover_url")
    private String coverUrl;
    @TableField("is_top")
    private Integer isTop;
    @TableField("status")
    private Integer status;
    @TableField("published_at")
    private LocalDateTime publishedAt;
    @TableField("create_by")
    private Long createBy;
    @TableField("update_by")
    private Long updateBy;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("deleted")
    private Integer deleted;
}
