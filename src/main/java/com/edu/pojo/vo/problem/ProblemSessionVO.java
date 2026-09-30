package com.edu.pojo.vo.problem;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemSessionVO {
    private String clientSessionId;
    private String wsToken;
    private Integer problemVersion;
    private String wsEndpoint;
}
