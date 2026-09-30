package com.edu.pojo.po.problem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("edu_problem_category")
public class ProblemCategoryPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long subjectId;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer status;
}
