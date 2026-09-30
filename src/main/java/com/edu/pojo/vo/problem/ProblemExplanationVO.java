package com.edu.pojo.vo.problem;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemExplanationVO {
    private String text;
    private boolean fallback;
}
