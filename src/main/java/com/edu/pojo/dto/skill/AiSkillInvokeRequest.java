package com.edu.pojo.dto.skill;

import lombok.Data;

@Data
public class AiSkillInvokeRequest {
    private String sourceModule;
    private String inputText;
}
