package com.edu.pojo.dto.skill;

import lombok.Data;

@Data
public class AiSkillCategorySaveRequest {
    private String categoryType;
    private String categoryName;
    private Integer sort;
    private Integer enabled;
}
