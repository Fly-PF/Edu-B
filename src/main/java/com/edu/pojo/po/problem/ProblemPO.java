package com.edu.pojo.po.problem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("edu_problem")
public class ProblemPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String title;
    private String intro;
    private String difficulty;
    private String tagsJson;
    private Long createBy;
    private Integer currentVersion;
    private Integer status;
}
