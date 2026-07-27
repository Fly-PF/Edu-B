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
public class AiFaceProfileVO {
    private Long id;
    private Long userId;
    private String userName;
    private String faceImageUrl;
    private String faceImageName;
    private String provider;
    private String faceModelVersion;
    private Integer compareCount;
    private Double lastCompareScore;
    private LocalDateTime lastCompareTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Boolean registered;
}
