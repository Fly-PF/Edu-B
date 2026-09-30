package com.edu.pojo.vo.skill;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiSkillPracticeOptionVO {
    private Long id;
    private String skillName;
    private String description;
    private String source;
}
