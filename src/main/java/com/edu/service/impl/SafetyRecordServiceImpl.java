package com.edu.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.safety.SafetyDashboardDTO;
import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;
import com.edu.pojo.dto.safety.SafetyRecordDTO;
import com.edu.pojo.dto.safety.SafetyReviewActionRequest;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyReviewStatus;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.pojo.po.safety.SafetyRecordPO;
import com.edu.repository.safety.SafetyRecordRepository;
import com.edu.service.TeacherClassService;
import com.edu.service.safety.SafetyRecordService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SafetyRecordServiceImpl implements SafetyRecordService {
    private static final int DEFAULT_DASHBOARD_DAYS = 7;
    private static final int MAX_DASHBOARD_DAYS = 30;
    private static final int RECENT_HIGH_RISK_LIMIT = 8;
    private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {};

    private final SafetyRecordRepository safetyRecordRepository;
    private final TeacherClassService teacherClassService;
    private final ObjectMapper objectMapper;

    @Override
    public Long recordEvaluation(SafetyGatewayRequest request, SafetyGatewayResponse response) {
        validate(request, response);
        SafetyRecordPO record = SafetyRecordPO.builder()
                .sourceModule(name(request.getSourceModule()))
                .scene(name(request.getScene()))
                .userRole(name(request.getUserRole()))
                .gradeLevel(name(request.getGradeLevel()))
                .userId(request.getUserId())
                .classId(request.getClassId())
                .courseId(request.getCourseId())
                .chapterId(request.getChapterId())
                .inputText(request.getInputText())
                .outputText(request.getOutputText())
                .allowed(response.isAllowed())
                .riskLevel(name(response.getRiskLevel()))
                .riskTypes(writeJson(response.getRiskTypes()))
                .decision(name(response.getDecision()))
                .reason(response.getReason())
                .suggestion(response.getSuggestion())
                .processedText(response.getProcessedText())
                .evidenceLevel(name(response.getEvidenceLevel()))
                .evidenceScore(response.getEvidenceScore())
                .manualReviewRequired(response.getManualReviewRequired())
                .reviewStatus(Boolean.TRUE.equals(response.getManualReviewRequired())
                        ? SafetyReviewStatus.PENDING.name()
                        : SafetyReviewStatus.NOT_REQUIRED.name())
                .reviewBy(null)
                .reviewByName(null)
                .reviewTime(null)
                .reviewComment(null)
                .metadataJson(writeJson(request.getMetadata()))
                .debugJson(writeJson(response.getDebugInfo()))
                .createTime(LocalDateTime.now())
                .build();
        int rows = safetyRecordRepository.insert(record);
        if (rows != 1 || record.getId() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist safety record");
        }
        return record.getId();
    }

    @Override
    public PageResult<SafetyRecordDTO> pageRecords(Integer pageNum,
                                                   Integer pageSize,
                                                   SafetySourceModule sourceModule,
                                                   SafetyScene scene,
                                                   SafetyUserRole userRole,
                                                   SafetyGradeLevel gradeLevel,
                                                   SafetyRiskLevel riskLevel,
                                                   SafetyRiskType riskType,
                                                   SafetyDecision decision,
                                                   SafetyReviewStatus reviewStatus,
                                                   Boolean manualReviewRequired,
                                                   String keyword) {
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<SafetyRecordPO> page = safetyRecordRepository.pageRecords(
                pageQuery,
                sourceModule,
                scene,
                userRole,
                gradeLevel,
                riskLevel,
                riskType,
                decision,
                reviewStatus,
                manualReviewRequired,
                keyword
        );
        List<SafetyRecordDTO> records = page.getRecords().stream().map(this::toDTO).toList();
        return PageResult.of(page.getTotal(), pageQuery, records);
    }

    @Override
    public PageResult<SafetyRecordDTO> pageReviewRecords(Integer pageNum,
                                                         Integer pageSize,
                                                         Long classId,
                                                         SafetySourceModule sourceModule,
                                                         SafetyScene scene,
                                                         SafetyUserRole userRole,
                                                         SafetyGradeLevel gradeLevel,
                                                         SafetyRiskLevel riskLevel,
                                                         SafetyRiskType riskType,
                                                         SafetyDecision decision,
                                                         SafetyReviewStatus reviewStatus,
                                                         Boolean manualReviewRequired,
                                                         String keyword) {
        UserInfoDTO loginUser = requireLoginUser();
        if (!isAdmin(loginUser)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "No permission to access manual review queue");
        }

        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<SafetyRecordPO> page = safetyRecordRepository.pageReviewRecords(
                pageQuery,
                classId,
                sourceModule,
                scene,
                userRole,
                gradeLevel,
                riskLevel,
                riskType,
                decision,
                reviewStatus,
                manualReviewRequired,
                keyword
        );
        List<SafetyRecordDTO> records = page.getRecords().stream().map(this::toDTO).toList();
        return PageResult.of(page.getTotal(), pageQuery, records);
    }

    @Override
    public SafetyRecordDTO detail(Long id) {
        SafetyRecordPO record = safetyRecordRepository.selectById(id);
        if (record == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "Safety record not found");
        }
        return toDTO(record);
    }

    @Override
    public SafetyRecordDTO reviewDetail(Long id) {
        SafetyRecordPO record = requireReviewRecord(id);
        return toDTO(record);
    }

    @Override
    public SafetyRecordDTO approveReview(Long id, SafetyReviewActionRequest request) {
        return reviewRecord(id, SafetyReviewStatus.APPROVED, request);
    }

    @Override
    public SafetyRecordDTO rejectReview(Long id, SafetyReviewActionRequest request) {
        return reviewRecord(id, SafetyReviewStatus.REJECTED, request);
    }

    @Override
    public SafetyDashboardDTO dashboard(Integer days) {
        int normalizedDays = normalizeDays(days);
        LocalDate startDate = LocalDate.now(ZoneId.systemDefault()).minusDays(normalizedDays - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        List<SafetyRecordPO> records = safetyRecordRepository.selectSince(startTime);
        List<SafetyRecordPO> todayRecords = records.stream()
                .filter(this::isToday)
                .toList();

        long total = records.size();
        long todayCount = todayRecords.size();
        long passCount = countDecision(records, SafetyDecision.PASS);
        long warnCount = countDecision(records, SafetyDecision.WARN);
        long blockCount = countDecision(records, SafetyDecision.BLOCK);
        long desensitizeCount = countDecision(records, SafetyDecision.DESENSITIZE);
        long rewriteCount = countDecision(records, SafetyDecision.REWRITE);
        long highRiskCount = countRiskLevel(records, SafetyRiskLevel.HIGH);
        long manualReviewCount = records.stream().filter(this::isPendingReview).count();
        long reviewPendingCount = records.stream().filter(record -> resolveReviewStatus(record) == SafetyReviewStatus.PENDING).count();
        long reviewApprovedCount = records.stream().filter(record -> resolveReviewStatus(record) == SafetyReviewStatus.APPROVED).count();
        long reviewRejectedCount = records.stream().filter(record -> resolveReviewStatus(record) == SafetyReviewStatus.REJECTED).count();
        long reviewNotRequiredCount = records.stream().filter(record -> resolveReviewStatus(record) == SafetyReviewStatus.NOT_REQUIRED).count();

        List<SafetyDashboardDTO.TrendItem> trendItems = buildTrend(records, startDate, normalizedDays);
        List<SafetyDashboardDTO.MetricItem> riskTypeItems = buildRiskTypeDistribution(records);
        List<SafetyDashboardDTO.MetricItem> sourceItems = buildDistribution(records, SafetyRecordPO::getSourceModule, this::sourceModuleLabel);
        List<SafetyDashboardDTO.MetricItem> gradeItems = buildDistribution(records, SafetyRecordPO::getGradeLevel, this::gradeLabel);
        List<SafetyRecordDTO> recentHighRiskRecords = safetyRecordRepository.selectRecentHighRisk(RECENT_HIGH_RISK_LIMIT)
                .stream()
                .map(this::toDTO)
                .toList();

        return SafetyDashboardDTO.builder()
                .totalRequests(total)
                .todayRequests(todayCount)
                .passCount(passCount)
                .warnCount(warnCount)
                .blockCount(blockCount)
                .desensitizeCount(desensitizeCount)
                .rewriteCount(rewriteCount)
                .highRiskCount(highRiskCount)
                .manualReviewCount(manualReviewCount)
                .reviewPendingCount(reviewPendingCount)
                .reviewApprovedCount(reviewApprovedCount)
                .reviewRejectedCount(reviewRejectedCount)
                .reviewNotRequiredCount(reviewNotRequiredCount)
                .riskTypeDistribution(riskTypeItems)
                .sourceModuleDistribution(sourceItems)
                .gradeDistribution(gradeItems)
                .dailyTrend(trendItems)
                .recentHighRiskRecords(recentHighRiskRecords)
                .build();
    }

    private void validate(SafetyGatewayRequest request, SafetyGatewayResponse response) {
        if (request == null || response == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "Safety record parameters cannot be null");
        }
    }

    private boolean isToday(SafetyRecordPO record) {
        return record.getCreateTime() != null && record.getCreateTime().toLocalDate().equals(LocalDate.now());
    }

    private long countDecision(List<SafetyRecordPO> records, SafetyDecision decision) {
        return records.stream().filter(record -> decision.name().equals(record.getDecision())).count();
    }

    private long countRiskLevel(List<SafetyRecordPO> records, SafetyRiskLevel riskLevel) {
        return records.stream().filter(record -> riskLevel.name().equals(record.getRiskLevel())).count();
    }

    private List<SafetyDashboardDTO.TrendItem> buildTrend(List<SafetyRecordPO> records, LocalDate startDate, int days) {
        Map<String, List<SafetyRecordPO>> grouped = records.stream()
                .filter(record -> record.getCreateTime() != null)
                .collect(Collectors.groupingBy(record -> record.getCreateTime().toLocalDate().toString(), LinkedHashMap::new, Collectors.toList()));

        List<SafetyDashboardDTO.TrendItem> items = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            List<SafetyRecordPO> dayRecords = grouped.getOrDefault(date.toString(), Collections.emptyList());
            long highRiskCount = dayRecords.stream().filter(record -> SafetyRiskLevel.HIGH.name().equals(record.getRiskLevel())).count();
            items.add(SafetyDashboardDTO.TrendItem.builder()
                    .date(date.toString())
                    .totalCount((long) dayRecords.size())
                    .highRiskCount(highRiskCount)
                    .build());
        }
        return items;
    }

    private List<SafetyDashboardDTO.MetricItem> buildRiskTypeDistribution(List<SafetyRecordPO> records) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (SafetyRecordPO record : records) {
            for (String type : readStringList(record.getRiskTypes())) {
                counts.merge(type, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .map(entry -> SafetyDashboardDTO.MetricItem.builder()
                        .code(entry.getKey())
                        .label(riskTypeLabel(entry.getKey()))
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private List<SafetyDashboardDTO.MetricItem> buildDistribution(List<SafetyRecordPO> records,
                                                                  Function<SafetyRecordPO, String> keyFn,
                                                                  Function<String, String> labelFn) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (SafetyRecordPO record : records) {
            String key = keyFn.apply(record);
            if (StringUtils.hasText(key)) {
                counts.merge(key, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .map(entry -> SafetyDashboardDTO.MetricItem.builder()
                        .code(entry.getKey())
                        .label(labelFn.apply(entry.getKey()))
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private String sourceModuleLabel(String key) {
        SafetySourceModule module = parseEnum(SafetySourceModule.class, key);
        if (module == null) {
            return key;
        }
        return switch (module) {
            case AI_COMPANION -> "智能学伴";
            case TEACHER_PREP -> "教师备课批改";
            case EDUCATION_RAG -> "教育RAG";
            case PROJECT_CASE -> "项目式案例库";
            case LEARNING_ANALYSIS -> "学情分析";
            case MULTIMODAL_TEACHING -> "多模态教学";
            case MANUAL_TEST -> "手动测试";
        };
    }

    private String gradeLabel(String key) {
        SafetyGradeLevel gradeLevel = parseEnum(SafetyGradeLevel.class, key);
        if (gradeLevel == null) {
            return key;
        }
        return switch (gradeLevel) {
            case PRIMARY -> "小学";
            case JUNIOR -> "初中";
            case SENIOR -> "高中";
        };
    }

    private String riskTypeLabel(String key) {
        SafetyRiskType type = parseEnum(SafetyRiskType.class, key);
        if (type == null) {
            return key;
        }
        return switch (type) {
            case HALLUCINATION -> "幻觉";
            case PRIVACY -> "隐私泄露";
            case CHEATING -> "诱导作弊";
            case AGE_INAPPROPRIATE -> "不适龄内容";
            case PROMPT_ATTACK -> "提示词攻击";
        };
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private UserInfoDTO requireLoginUser() {
        UserInfoDTO loginUser = SecurityUtil.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "Please login first");
        }
        return loginUser;
    }

    private boolean isAdmin(UserInfoDTO loginUser) {
        if (loginUser == null || loginUser.getRoleCode() == null) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(loginUser.getRoleCode())
                || "SUPERADMIN".equalsIgnoreCase(loginUser.getRoleCode());
    }

    private SafetyReviewStatus resolveReviewStatus(SafetyRecordPO record) {
        SafetyReviewStatus reviewStatus = parseEnum(SafetyReviewStatus.class, record.getReviewStatus());
        if (reviewStatus != null) {
            return reviewStatus;
        }
        if (Boolean.TRUE.equals(record.getManualReviewRequired())) {
            return SafetyReviewStatus.PENDING;
        }
        return SafetyReviewStatus.NOT_REQUIRED;
    }

    private boolean isPendingReview(SafetyRecordPO record) {
        return resolveReviewStatus(record) == SafetyReviewStatus.PENDING;
    }

    private SafetyRecordPO requireReviewRecord(Long id) {
        SafetyRecordPO record = safetyRecordRepository.selectById(id);
        if (record == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "Safety record not found");
        }
        UserInfoDTO loginUser = requireLoginUser();
        if (!isAdmin(loginUser)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "No permission to access this review record");
        }
        return record;
    }

    private SafetyRecordDTO reviewRecord(Long id, SafetyReviewStatus reviewStatus, SafetyReviewActionRequest request) {
        SafetyRecordPO record = requireReviewRecord(id);
        SafetyReviewStatus currentStatus = resolveReviewStatus(record);
        if (currentStatus != SafetyReviewStatus.PENDING) {
            throw new BaseException(HttpStatus.CONFLICT, "This record has already been reviewed");
        }

        UserInfoDTO loginUser = requireLoginUser();
        String reviewComment = request == null ? null : request.resolvedComment();
        int rows = safetyRecordRepository.updateReview(
                id,
                reviewStatus,
                loginUser.getUserId(),
                StringUtils.hasText(loginUser.getRealName()) ? loginUser.getRealName() : loginUser.getUsername(),
                reviewComment,
                LocalDateTime.now(),
                false
        );
        if (rows != 1) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save review result");
        }
        SafetyRecordPO updated = safetyRecordRepository.selectById(id);
        return toDTO(updated == null ? record : updated);
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyMap() : value);
        } catch (JsonProcessingException e) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize safety record JSON");
        }
    }

    private SafetyRecordDTO toDTO(SafetyRecordPO record) {
        return SafetyRecordDTO.builder()
                .id(record.getId())
                .sourceModule(parseEnum(SafetySourceModule.class, record.getSourceModule()))
                .scene(parseEnum(SafetyScene.class, record.getScene()))
                .userRole(parseEnum(SafetyUserRole.class, record.getUserRole()))
                .gradeLevel(parseEnum(SafetyGradeLevel.class, record.getGradeLevel()))
                .userId(record.getUserId())
                .classId(record.getClassId())
                .courseId(record.getCourseId())
                .chapterId(record.getChapterId())
                .inputText(record.getInputText())
                .outputText(record.getOutputText())
                .allowed(record.getAllowed())
                .riskLevel(parseEnum(SafetyRiskLevel.class, record.getRiskLevel()))
                .riskTypes(readStringList(record.getRiskTypes()).stream()
                        .map(value -> parseEnum(SafetyRiskType.class, value))
                        .filter(Objects::nonNull)
                        .toList())
                .decision(parseEnum(SafetyDecision.class, record.getDecision()))
                .reason(record.getReason())
                .suggestion(record.getSuggestion())
                .processedText(record.getProcessedText())
                .evidenceLevel(parseEnum(SafetyEvidenceLevel.class, record.getEvidenceLevel()))
                .evidenceScore(record.getEvidenceScore())
                .manualReviewRequired(record.getManualReviewRequired())
                .reviewStatus(resolveReviewStatus(record))
                .reviewBy(record.getReviewBy())
                .reviewByName(record.getReviewByName())
                .reviewTime(record.getReviewTime())
                .reviewComment(record.getReviewComment())
                .metadata(readMap(record.getMetadataJson(), STRING_MAP_TYPE))
                .debugInfo(readMap(record.getDebugJson(), OBJECT_MAP_TYPE))
                .createTime(record.getCreateTime())
                .build();
    }

    private <T> T readMap(String json, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return DEFAULT_DASHBOARD_DAYS;
        }
        return Math.min(days, MAX_DASHBOARD_DAYS);
    }
}
