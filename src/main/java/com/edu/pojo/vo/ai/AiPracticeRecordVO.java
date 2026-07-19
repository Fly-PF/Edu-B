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
public class AiPracticeRecordVO {
    private Long id;
    private Long caseId;
    private String caseName;
    private Long userId;
    private String userName;
    private String practiceType;
    private String inputText;
    private String fileUrl;
    private String fileName;
    private String answerText;
    private String note;
    private AiPracticeAiResultVO aiResult;
    private Integer score;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
