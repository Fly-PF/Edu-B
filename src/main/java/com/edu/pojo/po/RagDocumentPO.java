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
@TableName(value = "rag_document")
public class RagDocumentPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "kb_id")
    private Long kbId;

    @TableField(value = "doc_name")
    private String docName;

    @TableField(value = "doc_type")
    private String docType;

    @TableField(value = "description")
    private String description;

    @TableField(value = "file_url")
    private String fileUrl;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(value = "ext_json")
    private String extJson;

    @TableField(value = "deleted")
    private Integer deleted;
}
