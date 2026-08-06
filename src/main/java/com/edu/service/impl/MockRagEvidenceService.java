package com.edu.service.impl;

import com.edu.pojo.dto.safety.RagEvidenceReferenceDTO;
import com.edu.pojo.dto.safety.RagEvidenceRequest;
import com.edu.pojo.dto.safety.RagEvidenceResponse;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.vo.ai.AiCompanionMaterialExcerpt;
import com.edu.pojo.vo.course.ChapterVO;
import com.edu.pojo.vo.course.ResourceVO;
import com.edu.service.CourseMaterialRetrievalService;
import com.edu.service.CourseService;
import com.edu.service.safety.RagEvidenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MockRagEvidenceService implements RagEvidenceService {
    private static final List<String> EVIDENCE_HINT_PHRASES = List.of(
            "according to the course",
            "according to the material",
            "according to the source",
            "reference",
            "source",
            "rag",
            "根据课程资料",
            "根据教材",
            "根据来源",
            "依据课程",
            "有来源",
            "有引用"
    );
    private static final List<String> NO_EVIDENCE_HINT_PHRASES = List.of(
            "no source",
            "no evidence",
            "missing source",
            "cannot verify",
            "no citation",
            "no clear source"
    );
    private static final List<String> UNCERTAIN_HINT_PHRASES = List.of(
            "probably",
            "definitely",
            "research shows",
            "obviously",
            "certainly",
            "100%",
            "90%",
            "绝对",
            "一定能",
            "百分之百",
            "absolutely"
    );

    private CourseService courseService;
    private CourseMaterialRetrievalService materialRetrievalService;

    public MockRagEvidenceService() {
    }

    @Autowired
    public MockRagEvidenceService(CourseService courseService,
                                  CourseMaterialRetrievalService materialRetrievalService) {
        this.courseService = courseService;
        this.materialRetrievalService = materialRetrievalService;
    }

    @Override
    public RagEvidenceResponse checkEvidence(RagEvidenceRequest request) {
        if (request == null || !StringUtils.hasText(request.getAnswer())) {
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.NOT_CHECKED)
                    .reason("No AI output text provided")
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
                    .reason(firstNonBlank(valueOf(metadata, "ragEvidenceReason"), "Evidence level from metadata"))
                    .source("metadata")
                    .build();
        }

        RagEvidenceResponse courseEvidence = checkCourseMaterialEvidence(request, overrideScore);
        if (courseEvidence != null) {
            return courseEvidence;
        }

        String answer = request.getAnswer();
        if (containsAny(answer, NO_EVIDENCE_HINT_PHRASES)) {
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.UNSUPPORTED)
                    .score(Optional.ofNullable(overrideScore).orElse(0.25d))
                    .reason("Text clearly indicates missing source or verifiable evidence")
                    .source("mock-rag")
                    .build();
        }

        if (containsAny(answer, EVIDENCE_HINT_PHRASES)) {
            Double score = Optional.ofNullable(overrideScore).orElse(0.90d);
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.SUPPORTED)
                    .score(score)
                    .reason("Detected course source, citation, or knowledge-base support")
                    .source("mock-rag")
                    .references(List.of(RagEvidenceReferenceDTO.builder()
                            .title("Simulated RAG evidence")
                            .snippet("This is the safety evaluation center's RAG adapter result and can later be replaced by a real retrieval snippet.")
                            .sourceId("mock-rag")
                            .score(score)
                            .build()))
                    .build();
        }

        if (containsAny(answer, UNCERTAIN_HINT_PHRASES)) {
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.UNCERTAIN)
                    .score(Optional.ofNullable(overrideScore).orElse(0.55d))
                    .reason("Contains absolute or conclusion-heavy wording, but no clear source was found")
                    .source("mock-rag")
                    .build();
        }

        return RagEvidenceResponse.builder()
                .evidenceLevel(SafetyEvidenceLevel.UNSUPPORTED)
                .score(Optional.ofNullable(overrideScore).orElse(0.35d))
                .reason("No clear course source, citation, or knowledge-base evidence detected")
                .source("mock-rag")
                .build();
    }

    private RagEvidenceResponse checkCourseMaterialEvidence(RagEvidenceRequest request, Double overrideScore) {
        if (courseService == null || materialRetrievalService == null || request.getCourseId() == null) {
            return null;
        }

        try {
            List<ResourceVO> resources = loadCourseResources(request);
            if (resources.isEmpty()) {
                return null;
            }

            String query = firstNonBlank(request.getQuestion(), request.getAnswer());
            List<AiCompanionMaterialExcerpt> excerpts = materialRetrievalService.retrieve(resources, query);
            if (excerpts == null || excerpts.isEmpty()) {
                return RagEvidenceResponse.builder()
                        .evidenceLevel(SafetyEvidenceLevel.UNCERTAIN)
                        .score(Optional.ofNullable(overrideScore).orElse(0.50d))
                        .reason("Course resources did not yield enough supporting excerpts")
                        .source("course-material-retrieval")
                        .build();
            }

            Double score = Optional.ofNullable(overrideScore).orElse(0.86d);
            return RagEvidenceResponse.builder()
                    .evidenceLevel(SafetyEvidenceLevel.SUPPORTED)
                    .score(score)
                    .reason("Supporting excerpts were found in course resources")
                    .source("course-material-retrieval")
                    .references(excerpts.stream()
                            .limit(5)
                            .map(excerpt -> RagEvidenceReferenceDTO.builder()
                                    .title(firstNonBlank(excerpt.resourceName(), "Course resource"))
                                    .snippet(excerpt.content())
                                    .sourceId(excerpt.pageNumber() == null
                                            ? firstNonBlank(excerpt.resourceName(), "course-material")
                                            : firstNonBlank(excerpt.resourceName(), "course-material") + "#page-" + excerpt.pageNumber())
                                    .score(score)
                                    .build())
                            .toList())
                    .build();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private List<ResourceVO> loadCourseResources(RagEvidenceRequest request) {
        if (request.getChapterId() != null) {
            List<ResourceVO> resources = courseService.listChapterResources(request.getChapterId());
            return resources == null ? List.of() : resources;
        }

        List<ChapterVO> chapters = courseService.listCourseChapters(request.getCourseId());
        if (chapters == null || chapters.isEmpty()) {
            return List.of();
        }
        List<ResourceVO> resources = new ArrayList<>();
        for (ChapterVO chapter : chapters) {
            if (chapter != null && chapter.getId() != null) {
                List<ResourceVO> chapterResources = courseService.listChapterResources(chapter.getId());
                if (chapterResources != null) {
                    resources.addAll(chapterResources);
                }
            }
        }
        return resources;
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
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String valueOf(Map<String, String> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        return metadata.get(key);
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
}
