package com.edu.pojo.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDrawGuessPredictionVO {
    private String label;
    private Integer score;
}
