package com.edu.pojo.vo.skill;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AiSkillVO {
    private Long id;
    private Long userId;
    private String creatorName;
    private String skillName;
    private String description;
    private String skillType;
    private String subjectType;
    private String targetAudience;
    private String usageGuide;
    private String exampleInput;
    private String exampleOutput;
    private String visibility;
    private String status;
    private String reviewStatus;
    private String reviewMessage;
    private String rootObjectPath;
    private Boolean collected;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Builder.Default
    private List<AiSkillCategoryVO> categories = new ArrayList<>();

    @Builder.Default
    private List<AiSkillFileVO> files = new ArrayList<>();
}
