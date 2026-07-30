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
@TableName(value = "rag_chat_message")
public class RagChatMessagePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    private Long sessionId;

    @TableField(value = "message_id")
    private String messageId;

    @TableField(value = "role")
    private String role;

    @TableField(value = "content")
    private String content;

    @TableField(value = "metadata")
    private String metadata;

    @TableField(value = "doc_ref_count")
    private Integer docRefCount;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
