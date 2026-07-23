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
@TableName(value = "edu_ai_companion_session")
public class AiCompanionSessionPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "student_id")
    private Long studentId;

    @TableField(value = "course_id")
    private Long courseId;

    @TableField(value = "chapter_id")
    private Long chapterId;

    @TableField(value = "title")
    private String title;

    @TableField(value = "last_message_time")
    private LocalDateTime lastMessageTime;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(value = "deleted")
    private Integer deleted;
}
