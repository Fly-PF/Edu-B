package com.edu.service.safety;

import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.util.SecurityUtil;
import com.edu.util.SafetyGradeLevelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SafetyGatewaySupport {
    private final SafetyGatewayService safetyGatewayService;

    public String enforceInputText(SafetySourceModule sourceModule,
                                   SafetyScene scene,
                                   SafetyGradeLevel gradeLevel,
                                   String text,
                                   Map<String, String> metadata) {
        if (text == null || text.isBlank()) {
            return text;
        }

        SafetyGatewayResponse response = safetyGatewayService.evaluate(SafetyGatewayRequest.builder()
                .sourceModule(sourceModule)
                .scene(scene)
                .userRole(resolveUserRole())
                .gradeLevel(resolveGradeLevel(gradeLevel))
                .inputText(text)
                .recordLog(true)
                .metadata(copyMetadata(metadata))
                .build());

        if (response.getDecision() == SafetyDecision.BLOCK) {
            throw new UserErrorException(
                    HttpStatus.BAD_REQUEST,
                    firstNonBlank(response.getReason(), "内容未通过安全检测")
            );
        }

        String processedText = response.getProcessedText();
        if (processedText != null && !processedText.isBlank()) {
            return processedText;
        }
        return text;
    }

    private SafetyUserRole resolveUserRole() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getRoleCode() == null) {
            return SafetyUserRole.STUDENT;
        }
        String roleCode = user.getRoleCode().trim().toUpperCase();
        if ("ADMIN".equals(roleCode) || "SUPERADMIN".equals(roleCode)) {
            return SafetyUserRole.ADMIN;
        }
        if ("TEACHER".equals(roleCode)) {
            return SafetyUserRole.TEACHER;
        }
        return SafetyUserRole.STUDENT;
    }

    private SafetyGradeLevel resolveGradeLevel(SafetyGradeLevel fallback) {
        if (fallback != null) {
            return fallback;
        }
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user != null) {
            SafetyGradeLevel gradeLevel = SafetyGradeLevelResolver.resolve(user.getGrade());
            if (gradeLevel != null) {
                return gradeLevel;
            }
        }
        SafetyUserRole userRole = resolveUserRole();
        if (userRole == SafetyUserRole.TEACHER || userRole == SafetyUserRole.ADMIN) {
            return SafetyGradeLevel.SENIOR;
        }
        return SafetyGradeLevel.JUNIOR;
    }

    private Map<String, String> copyMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
