package com.edu.service.impl;

import com.edu.common.properties.SafetyRuleProperties;
import com.edu.exception.UserErrorException;
import com.edu.pojo.dto.safety.RagEvidenceRequest;
import com.edu.pojo.dto.safety.RagEvidenceResponse;
import com.edu.pojo.dto.safety.SafetyGatewayRequest;
import com.edu.pojo.dto.safety.SafetyGatewayResponse;
import com.edu.pojo.dto.safety.SemanticReviewRequest;
import com.edu.pojo.dto.safety.SemanticReviewResponse;
import com.edu.pojo.enums.safety.SafetyDecision;
import com.edu.pojo.enums.safety.SafetyEvidenceLevel;
import com.edu.pojo.enums.safety.SafetyGradeLevel;
import com.edu.pojo.enums.safety.SafetyRiskLevel;
import com.edu.pojo.enums.safety.SafetyRiskType;
import com.edu.pojo.enums.safety.SafetyScene;
import com.edu.pojo.enums.safety.SafetySourceModule;
import com.edu.pojo.enums.safety.SafetyUserRole;
import com.edu.service.safety.RagEvidenceService;
import com.edu.service.safety.SafetyGatewayService;
import com.edu.service.safety.SafetyRecordService;
import com.edu.service.safety.SemanticReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SafetyGatewayServiceImpl implements SafetyGatewayService {
    private final RagEvidenceService ragEvidenceService;
    private final SemanticReviewService semanticReviewService;
    private final SafetyRecordService safetyRecordService;
    private final SafetyRuleProperties ruleProperties;

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?86[- ]?)?1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<!\\d)(?:\\d{15}|\\d{17}[\\dXx])(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}\\b");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(?:住址|地址|家庭住址|详细地址|现住|居住地)[:：\\s]*([^，。；;\\n]{4,})");

    private static final List<String> DEFAULT_PRIVACY_COMPOSITE_PHRASES = List.of(
            "学校",
            "班级",
            "姓名",
            "学号",
            "家长电话",
            "家庭住址"
    );
    private static final List<String> DEFAULT_LEARNING_SUPPORT_PHRASES = List.of(
            "不要直接给答案",
            "不用直接给答案",
            "别直接给答案",
            "不要告诉我最终答案",
            "只讲思路",
            "讲解思路",
            "给我提示",
            "步骤提示",
            "知识点回顾",
            "帮我检查思路",
            "帮我找错误",
            "自查清单",
            "don't give me the answer",
            "explain the idea",
            "give me hints"
    );
    private static final List<String> DEFAULT_CHEATING_PHRASES = List.of(
            "直接给我答案",
            "直接给答案",
            "帮我直接写",
            "帮我写完",
            "帮我把作业",
            "帮我把报告",
            "帮我把论文",
            "作业写完",
            "代写",
            "写作业",
            "完成作业",
            "考试答案",
            "抄答案",
            "不要解释",
            "直接输出结果",
            "像我自己写",
            "像我写的",
            "不要让老师发现",
            "不让老师发现",
            "别让老师发现",
            "隐藏ai痕迹",
            "隐藏 ai 痕迹",
            "降低查重",
            "规避检测",
            "交作业用",
            "tell me the answer directly",
            "give me the final answer",
            "make it look like i wrote it",
            "avoid teacher detection"
    );
    private static final List<String> DEFAULT_CHEATING_EVASION_PHRASES = List.of(
            "像我自己写",
            "像我写的",
            "不要让老师发现",
            "不让老师发现",
            "别让老师发现",
            "隐藏ai痕迹",
            "隐藏 ai 痕迹",
            "降低查重",
            "规避检测",
            "绕过查重",
            "过查重",
            "查重率低",
            "反检测",
            "ai率",
            "AI率",
            "make it look like i wrote it",
            "avoid teacher detection",
            "avoid ai detection"
    );
    private static final List<String> DEFAULT_AGE_INAPPROPRIATE_PHRASES = List.of(
            "暴力",
            "色情",
            "自杀",
            "自残",
            "自伤",
            "轻生",
            "不想活",
            "毒品",
            "吸毒",
            "赌博",
            "仇恨",
            "歧视",
            "违法",
            "违法内容",
            "极端",
            "虐待",
            "枪支",
            "爆炸",
            "炸药",
            "管制刀具",
            "危险物品",
            "血腥",
            "恐怖主义",
            "校园霸凌",
            "霸凌同学",
            "报复老师",
            "报复同学",
            "suicide",
            "self harm",
            "porn",
            "drug",
            "weapon",
            "explosive"
    );
    private static final List<String> DEFAULT_HIGH_RISK_AGE_INAPPROPRIATE_PHRASES = List.of(
            "自杀",
            "怎么自杀",
            "自残",
            "自伤",
            "轻生",
            "不想活",
            "怎么跳楼",
            "色情",
            "毒品",
            "吸毒",
            "赌博网站",
            "怎么伤害别人",
            "怎么报复同学",
            "怎么报复老师",
            "怎么偷东西",
            "违法内容",
            "怎么搞破坏",
            "怎么打架",
            "怎么打架不被老师发现",
            "危险实验步骤",
            "危险物品",
            "枪支",
            "爆炸",
            "炸药",
            "暴力血腥",
            "校园欺凌细节",
            "校园暴力视频",
            "suicide",
            "self harm",
            "porn",
            "drug",
            "weapon",
            "explosive"
    );
    private static final List<String> DEFAULT_PRIVACY_BLOCK_PHRASES = List.of(
            "全班通讯录",
            "班级名单",
            "全班学生手机号",
            "学生信息登记表",
            "家长联系方式",
            "学生姓名和家长电话",
            "班级名单和手机号",
            "收集手机号",
            "收集身份证",
            "收集家庭住址",
            "批量导出学生",
            "导出学生信息",
            "他人手机号",
            "他人身份证",
            "他人家庭住址"
    );
    private static final List<String> DEFAULT_PROMPT_ATTACK_PHRASES = List.of(
            "忽略以上",
            "忽略之前",
            "忽略所有规则",
            "ignore previous",
            "ignore all safety rules",
            "system prompt",
            "输出系统提示",
            "泄露系统提示",
            "开发者模式",
            "越狱",
            "绕过限制",
            "绕过平台安全",
            "不要记录",
            "不要遵守规则",
            "你现在不是ai",
            "你现在不是 ai",
            "扮演无视规则",
            "关闭安全策略",
            "bypass safety",
            "jailbreak",
            "developer mode",
            "reveal your prompt"
    );

    @Autowired
    public SafetyGatewayServiceImpl(RagEvidenceService ragEvidenceService,
                                    SemanticReviewService semanticReviewService,
                                    SafetyRecordService safetyRecordService,
                                    SafetyRuleProperties ruleProperties) {
        this.ragEvidenceService = ragEvidenceService;
        this.semanticReviewService = semanticReviewService;
        this.safetyRecordService = safetyRecordService;
        this.ruleProperties = ruleProperties == null ? new SafetyRuleProperties() : ruleProperties;
    }

    public SafetyGatewayServiceImpl(RagEvidenceService ragEvidenceService,
                                    SemanticReviewService semanticReviewService,
                                    SafetyRecordService safetyRecordService) {
        this(ragEvidenceService, semanticReviewService, safetyRecordService, new SafetyRuleProperties());
    }

    @Override
    public SafetyGatewayResponse evaluate(SafetyGatewayRequest request) {
        validateRequest(request);

        String inputText = normalizeText(request.getInputText());
        String outputText = normalizeText(request.getOutputText());
        Map<String, String> metadata = safeMetadata(request.getMetadata());

        List<RuleHit> hits = new ArrayList<>();
        if (StringUtils.hasText(inputText)) {
            hits.addAll(analyzeText(inputText, "input", request));
        }
        if (StringUtils.hasText(outputText)) {
            hits.addAll(analyzeText(outputText, "output", request));
        }

        SemanticReviewResponse semanticReview = evaluateSemantics(request, inputText, outputText, metadata);
        hits.addAll(convertSemanticReviewToHits(semanticReview, request));

        EvidenceResult evidenceResult = evaluateEvidence(request, inputText, outputText, metadata);
        hits.addAll(analyzeEvidenceRisk(evidenceResult, request));

        List<RuleHit> mergedHits = new ArrayList<>(deduplicateHits(hits));
        mergedHits = filterTeacherCheatingHits(request, mergedHits);
        mergedHits = new ArrayList<>(mergedHits);
        mergedHits.sort(primaryHitComparator().reversed());

        RuleHit primaryHit = mergedHits.isEmpty() ? null : mergedHits.get(0);
        SafetyDecision decision = chooseDecision(mergedHits);
        SafetyRiskLevel riskLevel = chooseRiskLevel(mergedHits, evidenceResult);
        String processedText = buildProcessedText(inputText, outputText, decision, primaryHit);
        boolean allowed = decision != SafetyDecision.BLOCK;
        boolean manualReviewRequired = determineManualReviewRequired(mergedHits, evidenceResult, request);
        boolean teacherConfirmationRequired = request.getUserRole() == SafetyUserRole.TEACHER
                && allowed
                && decision != SafetyDecision.PASS;

        SafetyGatewayResponse response = SafetyGatewayResponse.builder()
                .allowed(allowed)
                .riskLevel(riskLevel)
                .riskTypes(mergedHits.stream()
                        .map(RuleHit::riskType)
                        .distinct()
                        .toList())
                .decision(decision)
                .reason(buildReason(primaryHit, mergedHits, evidenceResult))
                .suggestion(buildSuggestion(primaryHit, evidenceResult))
                .processedText(processedText)
                .evidenceLevel(evidenceResult.level())
                .evidenceScore(evidenceResult.score())
                .manualReviewRequired(manualReviewRequired)
                .teacherConfirmationRequired(teacherConfirmationRequired)
                .debugInfo(buildDebugInfo(request, inputText, outputText, mergedHits, evidenceResult, semanticReview))
                .build();
        Long recordId = request.shouldRecordLog() ? safetyRecordService.recordEvaluation(request, response) : null;
        return response.toBuilder()
                .recordId(recordId)
                .build();
    }

    private void validateRequest(SafetyGatewayRequest request) {
        if (request == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "安全检测请求不能为空");
        }
        if (request.getSourceModule() == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "来源模块不能为空");
        }
        if (request.getScene() == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "检测场景不能为空");
        }
        if (request.getUserRole() == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "用户角色不能为空");
        }
        if (request.getGradeLevel() == null) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "学段不能为空");
        }
        if (!request.hasContent()) {
            throw new UserErrorException(HttpStatus.BAD_REQUEST, "请输入待检测内容");
        }
    }

    private List<RuleHit> analyzeText(String text, String source, SafetyGatewayRequest request) {
        List<RuleHit> hits = new ArrayList<>();
        hits.addAll(analyzePrivacy(text, source));
        hits.addAll(analyzeCheating(text, source, request));
        hits.addAll(analyzeAgeInappropriate(text, source, request.getGradeLevel()));
        hits.addAll(analyzePromptAttack(text, source));
        return hits;
    }

    private List<RuleHit> analyzePrivacy(String text, String source) {
        Set<String> signals = new LinkedHashSet<>();
        if (PHONE_PATTERN.matcher(text).find()) {
            signals.add("手机号");
        }
        if (ID_CARD_PATTERN.matcher(text).find()) {
            signals.add("身份证号");
        }
        if (EMAIL_PATTERN.matcher(text).find()) {
            signals.add("邮箱");
        }
        if (ADDRESS_PATTERN.matcher(text).find()) {
            signals.add("地址");
        }
        if (containsPrivacyComposite(text)) {
            signals.add("学校班级姓名组合");
        }
        if (containsPrivacyBlockRisk(text)) {
            signals.add("批量或诱导收集隐私");
        }
        if (signals.isEmpty()) {
            return List.of();
        }

        boolean blockPrivacy = containsPrivacyBlockRisk(text);
        return List.of(new RuleHit(
                SafetyRiskType.PRIVACY,
                SafetyRiskLevel.HIGH,
                blockPrivacy ? SafetyDecision.BLOCK : SafetyDecision.DESENSITIZE,
                source,
                blockPrivacy
                        ? "检测到批量或诱导收集学生隐私信息: " + String.join("、", signals)
                        : "检测到敏感个人信息: " + String.join("、", signals),
                blockPrivacy
                        ? "请删除班级名单、通讯录、家长联系方式等批量隐私内容，仅保留必要的脱敏统计信息。"
                        : "请删除或脱敏手机号、身份证号、邮箱、地址等个人信息后再发送。",
                30
        ));
    }

    private List<RuleHit> analyzeCheating(String text, String source, SafetyGatewayRequest request) {
        if (request.getUserRole() != SafetyUserRole.STUDENT) {
            return List.of();
        }
        if (isLearningSupportRequest(text)) {
            return List.of();
        }
        if (!containsAny(text, cheatingPhrases())) {
            return List.of();
        }

        return List.of(new RuleHit(
                SafetyRiskType.CHEATING,
                SafetyRiskLevel.HIGH,
                SafetyDecision.BLOCK,
                source,
                "检测到诱导作弊或代写倾向。",
                "建议改为请求解题思路、步骤提示、知识点回顾或自查清单。",
                40
        ));
    }

    private List<RuleHit> analyzeAgeInappropriate(String text, String source, SafetyGradeLevel gradeLevel) {
        if (!containsAny(text, ageInappropriatePhrases())) {
            return List.of();
        }

        boolean highRisk = containsAny(text, highRiskAgeInappropriatePhrases());
        SafetyDecision decision = highRisk || gradeLevel == SafetyGradeLevel.PRIMARY
                ? SafetyDecision.BLOCK
                : SafetyDecision.WARN;
        return List.of(new RuleHit(
                SafetyRiskType.AGE_INAPPROPRIATE,
                SafetyRiskLevel.HIGH,
                decision,
                source,
                highRisk ? "检测到高危不适龄内容。" : "检测到不适龄内容。",
                highRisk
                        ? "请移除自伤、暴力、违法、色情或危险操作内容，必要时转交教师或监护人处理。"
                        : "请改写为适合当前学段的安全表达，必要时提交教师复审。",
                50
        ));
    }

    private List<RuleHit> analyzePromptAttack(String text, String source) {
        if (!containsAny(text, promptAttackPhrases())) {
            return List.of();
        }

        return List.of(new RuleHit(
                SafetyRiskType.PROMPT_ATTACK,
                SafetyRiskLevel.HIGH,
                SafetyDecision.BLOCK,
                source,
                "检测到提示词攻击或越权指令。",
                "请删除绕过限制、忽略规则、输出系统提示词等指令。",
                60
        ));
    }

    private SemanticReviewResponse evaluateSemantics(SafetyGatewayRequest request,
                                                     String inputText,
                                                     String outputText,
                                                     Map<String, String> metadata) {
        if (!StringUtils.hasText(inputText) && !StringUtils.hasText(outputText)) {
            return passSemanticReview();
        }

        SemanticReviewResponse response = semanticReviewService.review(SemanticReviewRequest.builder()
                .sourceModule(request.getSourceModule())
                .scene(request.getScene())
                .userRole(request.getUserRole())
                .gradeLevel(request.getGradeLevel())
                .inputText(inputText)
                .outputText(outputText)
                .metadata(metadata)
                .build());
        return response == null ? passSemanticReview() : normalizeSemanticReview(response);
    }

    private SemanticReviewResponse passSemanticReview() {
        return SemanticReviewResponse.builder()
                .decision(SafetyDecision.PASS)
                .riskLevel(SafetyRiskLevel.LOW)
                .confidence(0.0d)
                .source("semantic-empty")
                .build();
    }

    private SemanticReviewResponse normalizeSemanticReview(SemanticReviewResponse response) {
        SafetyDecision decision = response.getDecision() == null ? SafetyDecision.PASS : response.getDecision();
        SafetyRiskLevel riskLevel = response.getRiskLevel() == null ? inferRiskLevelFromDecision(decision) : response.getRiskLevel();
        List<SafetyRiskType> riskTypes = response.getRiskTypes() == null ? List.of() : response.getRiskTypes();
        if (decision == SafetyDecision.PASS && riskLevel != SafetyRiskLevel.LOW) {
            decision = SafetyDecision.WARN;
        }
        return response.toBuilder()
                .decision(decision)
                .riskLevel(riskLevel)
                .riskTypes(riskTypes)
                .reason(firstNonBlank(response.getReason(), "大模型语义审核未返回原因"))
                .suggestion(firstNonBlank(response.getSuggestion(), "请根据语义审核结果调整内容"))
                .source(firstNonBlank(response.getSource(), "semantic-review"))
                .build();
    }

    private List<RuleHit> convertSemanticReviewToHits(SemanticReviewResponse response,
                                                      SafetyGatewayRequest request) {
        if (response == null || response.getDecision() == SafetyDecision.PASS) {
            return List.of();
        }

        List<SafetyRiskType> riskTypes = response.getRiskTypes();
        if (riskTypes == null || riskTypes.isEmpty()) {
            riskTypes = List.of(inferRiskTypeFromSemanticReason(response.getReason()));
        }
        if (request != null && request.getUserRole() != SafetyUserRole.STUDENT) {
            riskTypes = riskTypes.stream()
                    .filter(type -> type != SafetyRiskType.CHEATING)
                    .toList();
        }
        if (riskTypes.isEmpty()) {
            return List.of();
        }

        List<RuleHit> hits = new ArrayList<>();
        for (SafetyRiskType riskType : riskTypes) {
            hits.add(new RuleHit(
                    riskType,
                    response.getRiskLevel() == null ? SafetyRiskLevel.MEDIUM : response.getRiskLevel(),
                    response.getDecision(),
                    "semantic",
                    response.getReason(),
                    response.getSuggestion(),
                    70
            ));
        }
        return hits;
    }

    private SafetyRiskType inferRiskTypeFromSemanticReason(String reason) {
        String normalized = StringUtils.hasText(reason) ? reason.toLowerCase(Locale.ROOT) : "";
        if (containsAny(normalized, List.of("作弊", "代写", "学术不端", "cheat"))) {
            return SafetyRiskType.CHEATING;
        }
        if (containsAny(normalized, List.of("隐私", "个人信息", "手机号", "身份证", "privacy"))) {
            return SafetyRiskType.PRIVACY;
        }
        if (containsAny(normalized, List.of("幻觉", "无据", "依据", "hallucination"))) {
            return SafetyRiskType.HALLUCINATION;
        }
        if (containsAny(normalized, List.of("提示词", "越权", "绕过", "prompt", "jailbreak"))) {
            return SafetyRiskType.PROMPT_ATTACK;
        }
        return SafetyRiskType.AGE_INAPPROPRIATE;
    }

    private EvidenceResult evaluateEvidence(SafetyGatewayRequest request,
                                            String inputText,
                                            String outputText,
                                            Map<String, String> metadata) {
        if (!StringUtils.hasText(outputText)) {
            return new EvidenceResult(SafetyEvidenceLevel.NOT_CHECKED, null, "未提供 AI 输出文本", "none");
        }

        RagEvidenceResponse response = ragEvidenceService.checkEvidence(RagEvidenceRequest.builder()
                .sourceModule(request.getSourceModule())
                .scene(request.getScene())
                .gradeLevel(request.getGradeLevel())
                .courseId(request.getCourseId())
                .chapterId(request.getChapterId())
                .question(inputText)
                .answer(outputText)
                .metadata(metadata)
                .build());

        SafetyEvidenceLevel level = response == null || response.getEvidenceLevel() == null
                ? SafetyEvidenceLevel.UNSUPPORTED
                : response.getEvidenceLevel();
        return new EvidenceResult(
                level,
                response == null ? null : response.getScore(),
                response == null ? "教育 RAG 证据校验未返回结果。" : response.getReason(),
                response == null ? "rag-adapter-empty" : response.getSource()
        );
    }

    private List<RuleHit> analyzeEvidenceRisk(EvidenceResult evidenceResult, SafetyGatewayRequest request) {
        if (evidenceResult.level() == SafetyEvidenceLevel.SUPPORTED
                || evidenceResult.level() == SafetyEvidenceLevel.NOT_CHECKED) {
            return List.of();
        }

        if (evidenceResult.level() == SafetyEvidenceLevel.UNCERTAIN) {
            return List.of(new RuleHit(
                    SafetyRiskType.HALLUCINATION,
                    SafetyRiskLevel.MEDIUM,
                    SafetyDecision.WARN,
                    "rag",
                    "AI 输出依据存疑。",
                    "建议补充课程来源、教材引用，或将结论改写为更保守的可验证表述。",
                    10
            ));
        }

        SafetyDecision decision = chooseUnsupportedEvidenceDecision(request);
        return List.of(new RuleHit(
                SafetyRiskType.HALLUCINATION,
                SafetyRiskLevel.HIGH,
                decision,
                "rag",
                "AI 输出缺少明确依据。",
                "建议补充课程来源、教材引用或知识库依据，再生成可验证的回答。",
                20
        ));
    }

    private SafetyDecision chooseUnsupportedEvidenceDecision(SafetyGatewayRequest request) {
        if (request.getGradeLevel() == SafetyGradeLevel.PRIMARY && request.getUserRole() == SafetyUserRole.STUDENT) {
            return SafetyDecision.BLOCK;
        }
        if (request.getGradeLevel() == SafetyGradeLevel.PRIMARY) {
            return SafetyDecision.WARN;
        }
        return SafetyDecision.REWRITE;
    }

    private SafetyDecision chooseDecision(List<RuleHit> hits) {
        return hits.stream()
                .map(RuleHit::decision)
                .max(Comparator.comparingInt(this::decisionWeight))
                .orElse(SafetyDecision.PASS);
    }

    private SafetyRiskLevel chooseRiskLevel(List<RuleHit> hits, EvidenceResult evidenceResult) {
        SafetyRiskLevel riskLevel = hits.stream()
                .map(RuleHit::riskLevel)
                .max(Comparator.comparingInt(this::riskLevelWeight))
                .orElse(SafetyRiskLevel.LOW);
        if (riskLevel == SafetyRiskLevel.LOW && evidenceResult.level() != SafetyEvidenceLevel.NOT_CHECKED) {
            return evidenceResult.level() == SafetyEvidenceLevel.SUPPORTED
                    ? SafetyRiskLevel.LOW
                    : evidenceResult.level() == SafetyEvidenceLevel.UNCERTAIN ? SafetyRiskLevel.MEDIUM : SafetyRiskLevel.HIGH;
        }
        return riskLevel;
    }

    private boolean determineManualReviewRequired(List<RuleHit> hits,
                                                  EvidenceResult evidenceResult,
                                                  SafetyGatewayRequest request) {
        if (request.getUserRole() == SafetyUserRole.TEACHER) {
            return false;
        }
        if (hits.stream().anyMatch(hit -> hit.decision() == SafetyDecision.WARN)) {
            return true;
        }
        return evidenceResult.level() != SafetyEvidenceLevel.NOT_CHECKED
                && evidenceResult.level() != SafetyEvidenceLevel.SUPPORTED
                && (request.getSourceModule() == SafetySourceModule.EDUCATION_RAG
                || request.getScene() == SafetyScene.AI_OUTPUT);
    }

    private String buildReason(RuleHit primaryHit, List<RuleHit> hits, EvidenceResult evidenceResult) {
        if (primaryHit != null) {
            StringJoiner joiner = new StringJoiner("；");
            joiner.add(primaryHit.reason());
            if (hits.size() > 1) {
                joiner.add("同时命中: " + hits.stream()
                        .map(RuleHit::riskType)
                        .distinct()
                        .filter(type -> type != primaryHit.riskType())
                        .map(Enum::name)
                        .collect(Collectors.joining("、")));
            }
            if (primaryHit.riskType() == SafetyRiskType.HALLUCINATION) {
                joiner.add(evidenceResult.note());
            }
            return joiner.toString();
        }
        if (evidenceResult.level() == SafetyEvidenceLevel.SUPPORTED) {
            return "内容通过安全检测，且证据可支撑。";
        }
        if (evidenceResult.level() == SafetyEvidenceLevel.NOT_CHECKED) {
            return "内容通过基础规则检测。";
        }
        return evidenceResult.note();
    }

    private String buildSuggestion(RuleHit primaryHit, EvidenceResult evidenceResult) {
        if (primaryHit != null) {
            return primaryHit.suggestion();
        }
        if (evidenceResult.level() == SafetyEvidenceLevel.UNCERTAIN) {
            return "建议补充课程来源、教材引用，或改写为更保守的表述。";
        }
        if (evidenceResult.level() == SafetyEvidenceLevel.UNSUPPORTED) {
            return "建议补充引用来源，或重新生成带依据的回答。";
        }
        return "可继续放行。";
    }

    private String buildProcessedText(String inputText,
                                      String outputText,
                                      SafetyDecision decision,
                                      RuleHit primaryHit) {
        String baseText = StringUtils.hasText(outputText) ? outputText : inputText;
        if (decision == SafetyDecision.BLOCK) {
            return buildBlockedText(primaryHit);
        }
        if (decision == SafetyDecision.DESENSITIZE) {
            return maskSensitiveInfo(baseText);
        }
        if (decision == SafetyDecision.REWRITE) {
            return buildRewriteText(primaryHit);
        }
        if (decision == SafetyDecision.WARN && primaryHit != null && primaryHit.riskType() == SafetyRiskType.HALLUCINATION) {
            return "该回答依据存疑，请补充课程来源或改写为可验证表述。";
        }
        return baseText;
    }

    private String buildBlockedText(RuleHit primaryHit) {
        if (primaryHit == null) {
            return "平台安全网关已拦截该请求。";
        }
        return switch (primaryHit.riskType()) {
            case CHEATING -> "平台已拦截该请求。建议改为拆解任务、梳理思路或给出知识点提示。";
            case AGE_INAPPROPRIATE -> "平台已拦截该请求。请删除不适龄内容后再继续。";
            case PROMPT_ATTACK -> "平台已拦截该请求。请移除越权或绕过限制的指令。";
            case PRIVACY -> "平台已识别敏感信息，请先脱敏后再发送。";
            case HALLUCINATION -> "平台已拦截该输出。请补充课程来源或改写为有依据的内容。";
        };
    }

    private String buildRewriteText(RuleHit primaryHit) {
        if (primaryHit == null) {
            return "建议补充来源后重新生成。";
        }
        return switch (primaryHit.riskType()) {
            case HALLUCINATION -> "请补充课程来源、教材引用或知识库依据，再生成可验证的回答。";
            case CHEATING -> "我不能直接代写答案，但可以帮助拆解题目、梳理思路和检查步骤。";
            case PRIVACY -> "请删除或脱敏个人信息后再继续。";
            case AGE_INAPPROPRIATE -> "请移除不适龄内容后再继续生成。";
            case PROMPT_ATTACK -> "请删除越权指令后重新提问。";
        };
    }

    private Map<String, Object> buildDebugInfo(SafetyGatewayRequest request,
                                               String inputText,
                                               String outputText,
                                               List<RuleHit> hits,
                                               EvidenceResult evidenceResult,
                                               SemanticReviewResponse semanticReview) {
        Map<String, Object> debugInfo = new LinkedHashMap<>();
        debugInfo.put("pipeline", List.of("rule-engine", "semantic-review", "rag-evidence", "decision-policy", "record-log"));
        debugInfo.put("sourceModule", request.getSourceModule());
        debugInfo.put("scene", request.getScene());
        debugInfo.put("userRole", request.getUserRole());
        debugInfo.put("gradeLevel", request.getGradeLevel());
        debugInfo.put("inputTextLength", StringUtils.hasText(inputText) ? inputText.length() : 0);
        debugInfo.put("outputTextLength", StringUtils.hasText(outputText) ? outputText.length() : 0);
        debugInfo.put("matchedRules", hits.stream()
                .map(hit -> hit.riskType().name() + ":" + hit.decision().name() + ":" + hit.source())
                .toList());
        debugInfo.put("semanticSource", semanticReview == null ? null : semanticReview.getSource());
        debugInfo.put("semanticDecision", semanticReview == null ? null : semanticReview.getDecision());
        debugInfo.put("semanticConfidence", semanticReview == null ? null : semanticReview.getConfidence());
        debugInfo.put("evidenceSource", evidenceResult.source());
        debugInfo.put("evidenceNote", evidenceResult.note());
        debugInfo.put("evidenceScore", evidenceResult.score());
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            debugInfo.put("metadataKeys", new ArrayList<>(request.getMetadata().keySet()));
        }
        return debugInfo;
    }

    private List<RuleHit> deduplicateHits(List<RuleHit> hits) {
        Map<SafetyRiskType, RuleHit> merged = new LinkedHashMap<>();
        for (RuleHit hit : hits) {
            merged.merge(hit.riskType(), hit, this::strongerHit);
        }
        return new ArrayList<>(merged.values());
    }

    private List<RuleHit> filterTeacherCheatingHits(SafetyGatewayRequest request, List<RuleHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return new ArrayList<>();
        }
        if (request == null || request.getUserRole() == SafetyUserRole.STUDENT) {
            return new ArrayList<>(hits);
        }
        return hits.stream()
                .filter(hit -> hit.riskType() != SafetyRiskType.CHEATING)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private RuleHit strongerHit(RuleHit first, RuleHit second) {
        int firstScore = decisionWeight(first.decision()) * 100 + first.priority();
        int secondScore = decisionWeight(second.decision()) * 100 + second.priority();
        return secondScore > firstScore ? second : first;
    }

    private Comparator<RuleHit> primaryHitComparator() {
        return Comparator
                .comparingInt((RuleHit hit) -> decisionWeight(hit.decision()))
                .thenComparingInt(RuleHit::priority)
                .thenComparing(hit -> hit.riskType().ordinal());
    }

    private int decisionWeight(SafetyDecision decision) {
        return switch (decision) {
            case BLOCK -> 5;
            case REWRITE -> 4;
            case DESENSITIZE -> 3;
            case WARN -> 2;
            case PASS -> 1;
        };
    }

    private int riskLevelWeight(SafetyRiskLevel riskLevel) {
        return switch (riskLevel) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private SafetyRiskLevel inferRiskLevelFromDecision(SafetyDecision decision) {
        return switch (decision) {
            case BLOCK, REWRITE, DESENSITIZE -> SafetyRiskLevel.HIGH;
            case WARN -> SafetyRiskLevel.MEDIUM;
            case PASS -> SafetyRiskLevel.LOW;
        };
    }

    private String maskSensitiveInfo(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String masked = PHONE_PATTERN.matcher(text).replaceAll("1**********");
        masked = ID_CARD_PATTERN.matcher(masked).replaceAll("******************");
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("masked@example.com");
        masked = ADDRESS_PATTERN.matcher(masked).replaceAll("地址已脱敏");
        return masked
                .replaceAll("(?:学校|班级|姓名)(?:[:：]?\\s*[^，。；;\\n]{1,12})", "学校/班级/姓名已脱敏")
                .trim();
    }

    private boolean containsPrivacyComposite(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        if (containsAll(text, List.of("学校", "班级", "姓名"))) {
            return true;
        }
        if (containsAll(text, List.of("姓名", "学号"))) {
            return true;
        }
        if (containsAll(text, List.of("学生", "家长电话"))) {
            return true;
        }
        return containsAny(text, configuredPhrases(ruleProperties.getPrivacyCompositePhrases()));
    }

    private boolean containsPrivacyBlockRisk(String text) {
        return containsAny(text, DEFAULT_PRIVACY_BLOCK_PHRASES)
                || (containsAny(text, configuredPhrases(ruleProperties.getPrivacyCompositePhrases()))
                && containsAny(text, List.of("全班", "名单", "通讯录", "登记表", "家长联系方式", "批量", "收集", "导出")));
    }

    private boolean isLearningSupportRequest(String text) {
        if (!containsAny(text, learningSupportPhrases())) {
            return false;
        }
        return !containsAny(text, cheatingEvasionPhrases())
                && !containsAny(text, List.of(
                "代写",
                "帮我写完",
                "作业写完",
                "考试答案",
                "抄答案",
                "交作业用",
                "降低查重",
                "规避检测",
                "隐藏ai痕迹",
                "隐藏 ai 痕迹"
        ));
    }

    private List<String> cheatingPhrases() {
        return mergePhrases(DEFAULT_CHEATING_PHRASES, ruleProperties.getCheatingPhrases());
    }

    private List<String> cheatingEvasionPhrases() {
        return mergePhrases(DEFAULT_CHEATING_EVASION_PHRASES, ruleProperties.getCheatingEvasionPhrases());
    }

    private List<String> learningSupportPhrases() {
        return mergePhrases(
                mergePhrases(DEFAULT_LEARNING_SUPPORT_PHRASES, ruleProperties.getLearningSupportPhrases()),
                ruleProperties.getAllowPhrases()
        );
    }

    private List<String> ageInappropriatePhrases() {
        return mergePhrases(
                mergePhrases(DEFAULT_AGE_INAPPROPRIATE_PHRASES, DEFAULT_HIGH_RISK_AGE_INAPPROPRIATE_PHRASES),
                ruleProperties.getAgeInappropriatePhrases()
        );
    }

    private List<String> highRiskAgeInappropriatePhrases() {
        return DEFAULT_HIGH_RISK_AGE_INAPPROPRIATE_PHRASES;
    }

    private List<String> promptAttackPhrases() {
        return mergePhrases(DEFAULT_PROMPT_ATTACK_PHRASES, ruleProperties.getPromptAttackPhrases());
    }

    private List<String> configuredPhrases(List<String> configured) {
        return configured == null ? List.of() : configured;
    }

    private List<String> mergePhrases(List<String> defaults, List<String> configured) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (defaults != null) {
            defaults.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(merged::add);
        }
        if (configured != null) {
            configured.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(merged::add);
        }
        return new ArrayList<>(merged);
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

    private boolean containsAll(String text, List<String> phrases) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String phrase : phrases) {
            if (!text.contains(phrase)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> safeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyMap();
        }
        return metadata;
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim().replaceAll("\\s+", " ");
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

    private record RuleHit(
            SafetyRiskType riskType,
            SafetyRiskLevel riskLevel,
            SafetyDecision decision,
            String source,
            String reason,
            String suggestion,
            int priority
    ) {
    }

    private record EvidenceResult(
            SafetyEvidenceLevel level,
            Double score,
            String note,
            String source
    ) {
    }
}
