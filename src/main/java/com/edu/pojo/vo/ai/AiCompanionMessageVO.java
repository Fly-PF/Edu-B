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
public class AiCompanionMessageVO {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Long chapterId;
    private Long resourceId;
    private String generationMode;
    private String modelName;
    private String sourceSummary;
    private String safetyStatus;
    private Long responseTimeMs;
    private LocalDateTime createTime;
}
