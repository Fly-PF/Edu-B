package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticReviewRequest implements Serializable {
    private SafetySourceModule sourceModule;
    private SafetyScene scene;
    private SafetyUserRole userRole;
    private SafetyGradeLevel gradeLevel;
    private String inputText;
    private String outputText;

    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();
}
