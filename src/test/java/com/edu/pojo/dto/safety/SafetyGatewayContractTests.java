package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyGatewayContractTests {

    @Test
    void requestShouldDetectPayloadPresence() {
        SafetyGatewayRequest request = SafetyGatewayRequest.builder()
                .sourceModule(SafetySourceModule.AI_COMPANION)
                .scene(SafetyScene.STUDENT_AI)
                .userRole(SafetyUserRole.STUDENT)
                .gradeLevel(SafetyGradeLevel.PRIMARY)
                .inputText("帮我直接写答案")
                .build();

        assertTrue(request.hasInputText());
        assertFalse(request.hasOutputText());
        assertTrue(request.hasContent());
    }

    @Test
    void responseShouldKeepProtocolDefaults() {
        SafetyGatewayResponse response = SafetyGatewayResponse.builder()
                .allowed(false)
                .riskLevel(SafetyRiskLevel.HIGH)
                .riskTypes(List.of(SafetyRiskType.CHEATING))
                .decision(SafetyDecision.BLOCK)
                .reason("诱导作弊")
                .suggestion("请改为拆解思路")
                .evidenceLevel(SafetyEvidenceLevel.NOT_CHECKED)
                .build();

        assertFalse(response.isAllowed());
        assertEquals(SafetyRiskLevel.HIGH, response.getRiskLevel());
        assertEquals(SafetyDecision.BLOCK, response.getDecision());
        assertEquals(SafetyEvidenceLevel.NOT_CHECKED, response.getEvidenceLevel());
        assertEquals(1, response.getRiskTypes().size());
        assertEquals(SafetyRiskType.CHEATING, response.getRiskTypes().get(0));
    }
}
