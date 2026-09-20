package com.edu.pojo.vo.skill;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiSkillFileVO {
    private String fileName;
    private Boolean isInReferences;
    private String objectPath;
    private String content;
}
