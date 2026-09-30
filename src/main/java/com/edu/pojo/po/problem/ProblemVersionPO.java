package com.edu.pojo.po.problem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("edu_problem_version")
public class ProblemVersionPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private Integer version;
    private String definitionJson;
    private String standardAnswer;
    private String answerAnalysis;
    private Integer status;
}
