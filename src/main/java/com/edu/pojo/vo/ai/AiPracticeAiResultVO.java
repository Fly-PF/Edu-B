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
public class AiPracticeAiResultVO {
    private String title;
    private String summary;
    private List<String> highlights;
    private List<String> suggestions;
    private List<String> nextSteps;
}
