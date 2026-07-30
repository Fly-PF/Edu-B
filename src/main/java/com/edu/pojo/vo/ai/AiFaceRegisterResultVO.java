package com.edu.pojo.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFaceRegisterResultVO {
    private AiFaceProfileVO profile;
    private Integer detectedFaceCount;
    private String provider;
    private String requestId;
    private String message;
}
