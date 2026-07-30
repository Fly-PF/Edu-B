package com.edu.pojo.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDrawGuessResultVO {
    private String provider;
    private String model;
    private String summary;
    private List<AiDrawGuessPredictionVO> predictions;
}
