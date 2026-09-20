package com.edu.pojo.dto.skill;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiSkillSaveRequest {
    private String skillName;
    private String description;
    private String skillType;
    private String subjectType;
    private String targetAudience;
    private String usageGuide;
    private String exampleInput;
    private String exampleOutput;
    private String visibility;
    private List<Long> categoryIds = new ArrayList<>();
    private List<AiSkillFileDTO> skillFiles = new ArrayList<>();
}
