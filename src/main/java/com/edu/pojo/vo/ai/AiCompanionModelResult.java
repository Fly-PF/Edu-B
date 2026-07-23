package com.edu.pojo.vo.ai;

public record AiCompanionModelResult(
        String content,
        String mode,
        String modelName,
        String sourceSummary,
        String safetyStatus
) {
}
