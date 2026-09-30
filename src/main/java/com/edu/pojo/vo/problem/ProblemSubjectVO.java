package com.edu.pojo.vo.problem;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemSubjectVO {
    private Long id;
    private String code;
    private String name;
}
