package com.edu.pojo.vo.skill;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiSkillCategoryVO {
    private Long id;
    private String categoryType;
    private String categoryName;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
