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
@TableName(value = "rag_session_kb_ref")
public class RagSessionKbRefPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    private Long sessionId;

    @TableField(value = "kb_id")
    private Long kbId;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
