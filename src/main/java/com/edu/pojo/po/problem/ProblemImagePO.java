package com.edu.pojo.po.problem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("edu_problem_image")
public class ProblemImagePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private String objectKey;
    private String originalFilename;
    private Integer sortOrder;
}
