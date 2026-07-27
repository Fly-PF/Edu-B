package com.edu.pojo.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFaceCompareResultVO {
    private Boolean matched;
    private Double score;
    private Double threshold;
    private String provider;
    private String requestId;
    private String message;
    private String compareImageUrl;
    private LocalDateTime compareAt;
    private AiFaceProfileVO profile;
}
