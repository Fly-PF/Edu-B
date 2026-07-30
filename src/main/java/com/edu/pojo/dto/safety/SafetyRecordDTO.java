package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetyReviewStatus;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyRecordDTO {
    private Long id;
    private SafetySourceModule sourceModule;
    private SafetyScene scene;
    private SafetyUserRole userRole;
    private SafetyGradeLevel gradeLevel;
    private Long userId;
    private Long classId;
    private Long courseId;
    private Long chapterId;
    private String inputText;
    private String outputText;
    private Boolean allowed;
    private SafetyRiskLevel riskLevel;

    @Builder.Default
    private List<SafetyRiskType> riskTypes = new ArrayList<>();

    private SafetyDecision decision;
    private String reason;
    private String suggestion;
    private String processedText;
    private SafetyEvidenceLevel evidenceLevel;
    private Double evidenceScore;
    private Boolean manualReviewRequired;
    private SafetyReviewStatus reviewStatus;
    private Long reviewBy;
    private String reviewByName;
    private LocalDateTime reviewTime;
    private String reviewComment;

    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Object> debugInfo = new LinkedHashMap<>();

    private LocalDateTime createTime;
}
