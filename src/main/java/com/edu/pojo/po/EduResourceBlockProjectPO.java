package com.edu.pojo.po;

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
@TableName("edu_resource_block_project")
public class EduResourceBlockProjectPO {
    @TableId("resource_id")
    private Long resourceId;
    @TableField("project_id")
    private Long projectId;
    @TableField("create_time")
    private LocalDateTime createTime;
}
