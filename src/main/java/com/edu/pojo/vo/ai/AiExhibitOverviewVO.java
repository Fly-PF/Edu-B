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
public class AiExhibitOverviewVO {
    private String productPositioning;
    private List<String> policyRequirements;
    private List<String> coreModules;
    private List<String> caseDataStructure;
    private List<String> caseFields;
    private List<String> reservedBackendApis;
    private List<String> frontendPages;
    private List<String> expansionDirections;
    private Long totalCases;
    private Long myPracticeCount;
}
