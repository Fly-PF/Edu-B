package com.edu.pojo.po.gov;

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
@TableName("edu_gov_knowledge_node")
public class GovKnowledgeNodePO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("subject")
    private String subject;

    @TableField("parent_id")
    private Long parentId;

    @TableField("node_type")
    private String nodeType;

    @TableField("title")
    private String title;

    @TableField("content_md")
    private String contentMd;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("status")
    private Integer status;

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
