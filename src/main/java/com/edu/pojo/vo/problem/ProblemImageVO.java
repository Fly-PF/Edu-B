package com.edu.pojo.vo.problem;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemImageVO {
    private Long id;
    private String originalFilename;
    private String url;
}
