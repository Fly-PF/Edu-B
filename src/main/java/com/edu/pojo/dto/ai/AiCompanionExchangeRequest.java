package com.edu.pojo.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCompanionExchangeRequest {
    private String question;
    private String answer;
    private Long chapterId;
    private Long resourceId;
}
