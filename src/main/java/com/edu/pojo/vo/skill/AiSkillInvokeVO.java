package com.edu.pojo.vo.skill;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiSkillInvokeVO {
    private Long skillId;
    private String skillName;
    private String sourceModule;
    private String inputText;
    private String assembledPrompt;
}
