package com.edu.service.impl;

import com.edu.pojo.dto.safety.RagEvidenceReferenceDTO;
import com.edu.pojo.dto.safety.RagEvidenceRequest;
import com.edu.pojo.dto.safety.RagEvidenceResponse;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.service.safety.RagEvidenceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MockRagEvidenceService implements RagEvidenceService {
    private static final List<String> EVIDENCE_HINT_PHRASES = List.of(
            "根据课程",
            "根据教材",
            "根据资料",
            "引用",
            "来源",
            "知识库",
            "出处",
            "rag"
    );
    private static final List<String> NO_EVIDENCE_HINT_PHRASES = List.of(
            "没有来源",
            "无来源",
            "缺少来源",
            "没有出处",
            "无出处",
            "未提供依据",
            "没有任何来源",
            "没有明确来源"
    );
    private static final List<String> UNCERTAIN_HINT_PHRASES = List.of(
            "一定",
            "绝对",
            "毫无疑问",
            "研究表明",
            "显然",
            "必然",
            "肯定",
            "百分之百"
    );

    @Override
    public RagEvidenceResponse checkEvidence(RagEvidenceRequest request) {
        if (request == null || !StringUtils.hasText(request.getAnswer())) {
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.NOT_CHECKED)
                    .reason("未提供 AI 输出文本")
                    .source("none")
                    .build();
        }

        Map<String, String> metadata = request.getMetadata();
        SafetyEvidenceLevel overrideLevel = parseEvidenceLevel(firstNonBlank(
                valueOf(metadata, "ragEvidenceLevel"),
                valueOf(metadata, "evidenceLevel"),
                valueOf(metadata, "evidence_level")
        ));
        Double overrideScore = parseDouble(firstNonBlank(
                valueOf(metadata, "ragEvidenceScore"),
                valueOf(metadata, "evidenceScore"),
                valueOf(metadata, "evidence_score")
        ));

        if (overrideLevel != null) {
            return RagEvidenceResponse.builder()
                    .evidenceLevel(overrideLevel)
                    .score(overrideScore)
                    .reason(firstNonBlank(valueOf(metadata, "ragEvidenceReason"), "来自教育 RAG 元数据的证据等级"))
                    .source("metadata")
                    .build();
        }

        String answer = request.getAnswer();
        if (containsAny(answer, NO_EVIDENCE_HINT_PHRASES)) {
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.UNSUPPORTED)
                    .score(Optional.ofNullable(overrideScore).orElse(0.25d))
                    .reason("文本明确表示缺少来源、出处或可核验证据")
                    .source("mock-rag")
                    .build();
        }

        if (containsAny(answer, EVIDENCE_HINT_PHRASES)) {
            Double score = Optional.ofNullable(overrideScore).orElse(0.90d);
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.SUPPORTED)
                    .score(score)
                    .reason("检测到课程来源、教材引用或知识库依据提示")
                    .source("mock-rag")
                    .references(List.of(RagEvidenceReferenceDTO.builder()
                            .title("教育 RAG 模拟证据")
                            .snippet("当前为安全评测中心的 RAG 适配层模拟结果，后续可替换为教育 RAG 的真实检索片段。")
                            .sourceId("mock-rag")
                            .score(score)
                            .build()))
                    .build();
        }

        if (containsAny(answer, UNCERTAIN_HINT_PHRASES)) {
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.UNCERTAIN)
                    .score(Optional.ofNullable(overrideScore).orElse(0.55d))
                    .reason("存在绝对化或结论化表述，但未发现明确来源")
                    .source("mock-rag")
                    .build();
        }

        return RagEvidenceResponse.builder()
                .evidenceLevel(SafetyEvidenceLevel.UNSUPPORTED)
                .score(Optional.ofNullable(overrideScore).orElse(0.35d))
                .reason("未发现明确课程来源、教材引用或知识库依据")
                .source("mock-rag")
                .build();
    }

    private boolean containsAny(String text, List<String> phrases) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String phrase : phrases) {
            if (normalized.contains(phrase.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String valueOf(Map<String, String> metadata, String key) {
        return metadata == null ? null : metadata.get(key);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private SafetyEvidenceLevel parseEvidenceLevel(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return SafetyEvidenceLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
