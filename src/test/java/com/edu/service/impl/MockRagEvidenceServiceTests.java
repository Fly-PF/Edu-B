package com.edu.service.impl;

import com.edu.pojo.dto.safety.RagEvidenceRequest;
import com.edu.pojo.dto.safety.RagEvidenceResponse;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MockRagEvidenceServiceTests {
    private final MockRagEvidenceService service = new MockRagEvidenceService();

    @Test
    void shouldUseMetadataEvidenceLevelWhenProvided() {
        RagEvidenceResponse response = service.checkEvidence(RagEvidenceRequest.builder()
                .answer("模型输出文本")
                .metadata(Map.of("ragEvidenceLevel", "SUPPORTED", "ragEvidenceScore", "0.92"))
                .build());

        assertEquals(SafetyEvidenceLevel.SUPPORTED, response.getEvidenceLevel());
        assertEquals(0.92d, response.getScore());
        assertEquals("metadata", response.getSource());
    }

    @Test
    void shouldReturnSupportedWhenAnswerContainsEvidenceHint() {
        RagEvidenceResponse response = service.checkEvidence(RagEvidenceRequest.builder()
                .answer("根据课程资料，本章介绍了生成式 AI 的基本概念。")
                .build());

        assertEquals(SafetyEvidenceLevel.SUPPORTED, response.getEvidenceLevel());
        assertFalse(response.getReferences().isEmpty());
    }

    @Test
    void shouldReturnUnsupportedWhenAnswerHasNoEvidence() {
        RagEvidenceResponse response = service.checkEvidence(RagEvidenceRequest.builder()
                .answer("这个结论没有任何来源说明。")
                .build());

        assertEquals(SafetyEvidenceLevel.UNSUPPORTED, response.getEvidenceLevel());
        assertEquals("mock-rag", response.getSource());
    }
}
