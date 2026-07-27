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
public class AiFaceCompareRecordVO {
    private Long id;
    private Double score;
    private Double threshold;
    private Boolean matched;
    private String provider;
    private String requestId;
    private String compareImageUrl;
    private LocalDateTime createdTime;
}
