package com.edu.pojo.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCompanionSessionCreateRequest {
    private Long courseId;
    private Long chapterId;
    private String title;
}
