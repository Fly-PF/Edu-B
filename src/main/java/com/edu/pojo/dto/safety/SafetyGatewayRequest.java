package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SafetyGatewayRequest implements Serializable {
    @NotNull
    private SafetySourceModule sourceModule;

    @NotNull
    private SafetyScene scene;

    @NotNull
    private SafetyUserRole userRole;

    @NotNull
    private SafetyGradeLevel gradeLevel;

    private Long userId;
    private Long classId;
    private Long courseId;
    private Long chapterId;

    private String inputText;
    private String outputText;
    private Boolean recordLog;

    @Builder.Default
    private Map<String, String> metadata = new LinkedHashMap<>();

    public boolean hasInputText() {
        return inputText != null && !inputText.isBlank();
    }

    public boolean hasOutputText() {
        return outputText != null && !outputText.isBlank();
    }

    public boolean hasContent() {
        return hasInputText() || hasOutputText();
    }

    public boolean shouldRecordLog() {
        return recordLog == null || recordLog;
    }
}
