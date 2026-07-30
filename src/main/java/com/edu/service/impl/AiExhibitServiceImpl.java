package com.edu.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.po.AiPracticeRecordPO;
import com.edu.pojo.po.AiProjectCasePO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.vo.ai.AiExhibitOverviewVO;
import com.edu.pojo.vo.ai.AiPracticeAiResultVO;
import com.edu.pojo.vo.ai.AiPracticeRecordVO;
import com.edu.pojo.vo.ai.AiProjectCaseVO;
import com.edu.pojo.vo.ai.AiRubricItemVO;
import com.edu.repository.AiExhibitRepository;
import com.edu.repository.SysUserRepository;
import com.edu.service.AiExhibitService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AiExhibitServiceImpl implements AiExhibitService {
    private static final int RECORD_STATUS_SUBMITTED = 1;

    private final AiExhibitRepository aiExhibitRepository;
    private final SysUserRepository sysUserRepository;
    private final AiExhibitStorageService storageService;
    private final ObjectMapper objectMapper;

    @Override
    public AiExhibitOverviewVO getOverview() {
        UserInfoDTO user = currentUser();
        long totalCases = aiExhibitRepository.selectEnabledCases().size();
        long practiceCount = aiExhibitRepository.selectPracticePage(1, 1, user.getUserId(), null).getTotal();

        return AiExhibitOverviewVO.builder()
                .productPositioning("面向中小学生的 AI 项目式学习案例库与在线实践平台，围绕真实问题、现成模型调用和可交付作品，帮助学生把 AI 学习从“知道”推进到“做出来”。")
                .policyRequirements(List.of(
                        "落实人工智能教育普及与分层推进要求，支持学生形成 AI 基础认知、应用意识和实践能力。",
                        "强化项目式、任务式、探究式学习，推动真实问题驱动的学习活动落地。",
                        "提供优质数字教育资源与可复用案例，促进区域、学校和课堂的资源共享。",
                        "支持学生在安全、可控、合规的环境中体验大模型、视觉模型、OCR 和语音识别等能力。",
                        "鼓励教师基于学情组织跨学科实践，培养创新思维、计算思维和数字素养。"
                ))
                .coreModules(List.of(
                        "案例库：按学段、学科方向、AI 能力、实践类型筛选与浏览。",
                        "案例详情：展示项目背景、学习目标、任务步骤、工具、示例代码和评价 Rubric。",
                        "在线实践：学生提交文本、图片或代码附件，系统返回 AI 演示结果并保存记录。",
                        "提交记录：查看个人提交历史、作品反馈和评分建议。",
                        "教师扩展：后续可接入案例共建、课堂布置、作业点评和数据统计。"
                ))
                .caseDataStructure(List.of(
                        "项目案例主表：保存案例基础信息、适合年级、学科方向、AI 能力、实践类型和展示顺序。",
                        "案例内容字段：学习目标、任务步骤、所需工具、示例代码、提交要求和评价 Rubric 用 JSON 存储。",
                        "实践记录表：保存学生输入、附件、AI 返回结果、评分、反馈和提交时间。",
                        "附件存储：图片、代码、文档和压缩包统一保存到本地 uploads 目录，前端通过公开地址读取。"
                ))
                .caseFields(List.of(
                        "项目名称",
                        "适合年级",
                        "学科方向",
                        "项目背景",
                        "学习目标",
                        "AI 能力",
                        "实践类型",
                        "任务步骤",
                        "所需工具",
                        "示例代码",
                        "提交作品要求",
                        "评价标准"
                ))
                .reservedBackendApis(List.of(
                        "GET /api/ai-exhibit/overview",
                        "GET /api/ai-exhibit/cases",
                        "GET /api/ai-exhibit/cases/{caseId}",
                        "GET /api/ai-exhibit/records",
                        "POST /api/ai-exhibit/cases/{caseId}/records"
                ))
                .frontendPages(List.of(
                        "AI展馆首页：政策概览、模块概览和案例库入口。",
                        "案例详情页：案例卡片、Rubric、示例代码和实践提交表单。",
                        "提交记录区：按案例查看学生提交历史和 AI 反馈。",
                        "后续可扩展教师管理页、案例共建页和数据看板。"
                ))
                .expansionDirections(List.of(
                        "接入真实大模型、视觉模型、OCR 和语音识别 API。",
                        "增加教师共建、审核和发布流程。",
                        "接入 Python 沙箱，支持学生运行代码并查看结果。",
                        "沉淀作品集、竞赛选题和科研课题数据。",
                        "形成区域级 AI 教育资源库和跨校协同实践社区。"
                ))
                .totalCases(totalCases)
                .myPracticeCount(practiceCount)
                .build();
    }

    @Override
    public PageResult<AiProjectCaseVO> listCases(
            Integer pageNum,
            Integer pageSize,
            String keyword,
            String gradeBand,
            String subjectDirection,
            String practiceType
    ) {
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<AiProjectCasePO> page = aiExhibitRepository.selectCasePage(
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                keyword,
                gradeBand,
                subjectDirection,
                practiceType
        );
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream().map(this::toCaseVO).toList());
    }

    @Override
    public AiProjectCaseVO getCase(Long caseId) {
        return toCaseVO(requireCase(caseId));
    }

    @Override
    public PageResult<AiPracticeRecordVO> listMyRecords(Integer pageNum, Integer pageSize, Long caseId) {
        UserInfoDTO user = currentUser();
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        IPage<AiPracticeRecordPO> page = aiExhibitRepository.selectPracticePage(
                pageQuery.getPageNum(),
                pageQuery.getPageSize(),
                user.getUserId(),
                caseId
        );
        Map<Long, AiProjectCasePO> caseMap = caseMap();
        return PageResult.of(page.getTotal(), pageQuery, page.getRecords().stream()
                .map(record -> toRecordVO(record, caseMap.get(record.getCaseId())))
                .toList());
    }

    @Override
    @Transactional
    public AiPracticeRecordVO submitPractice(
            Long caseId,
            String practiceType,
            String inputText,
            String answerText,
            String note,
            MultipartFile file
    ) {
        UserInfoDTO user = currentUser();
        AiProjectCasePO projectCase = requireCase(caseId);
        AiExhibitStorageService.StoredAiFile storedFile = null;
        if (file != null && !file.isEmpty()) {
            storedFile = storageService.upload(caseId, file);
        }

        String finalPracticeType = StringUtils.hasText(practiceType)
                ? practiceType.trim()
                : projectCase.getPracticeType();
        AiPracticeAiResultVO aiResult = generateAiResult(projectCase, finalPracticeType, inputText, answerText, note, storedFile);
        Integer score = calculateScore(projectCase, inputText, answerText, storedFile, aiResult);
        LocalDateTime now = LocalDateTime.now();

        AiPracticeRecordPO record = AiPracticeRecordPO.builder()
                .caseId(projectCase.getId())
                .userId(user.getUserId())
                .userName(user.getRealName())
                .practiceType(finalPracticeType)
                .inputText(inputText)
                .fileUrl(storedFile == null ? null : storedFile.readUrl())
                .fileName(storedFile == null ? null : storedFile.originalName())
                .answerText(answerText)
                .note(note)
                .aiResultJson(writeJson(aiResult))
                .score(score)
                .status(RECORD_STATUS_SUBMITTED)
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .extJson(writeMetaJson(projectCase, storedFile))
                .build();
        aiExhibitRepository.insertPracticeRecord(record);
        return toRecordVO(record, projectCase);
    }

    private AiProjectCaseVO toCaseVO(AiProjectCasePO po) {
        return AiProjectCaseVO.builder()
                .id(po.getId())
                .projectCode(po.getProjectCode())
                .projectName(po.getProjectName())
                .caseSummary(po.getCaseSummary())
                .gradeBand(po.getGradeBand())
                .subjectDirection(po.getSubjectDirection())
                .projectBackground(po.getProjectBackground())
                .learningGoals(readStringList(po.getLearningGoalsJson()))
                .aiCapability(po.getAiCapability())
                .practiceType(po.getPracticeType())
                .taskSteps(readStringList(po.getTaskStepsJson()))
                .requiredTools(readStringList(po.getRequiredToolsJson()))
                .exampleCode(po.getExampleCode())
                .submissionRequirements(po.getSubmissionRequirements())
                .evaluationRubric(readRubricList(po.getEvaluationRubricJson()))
                .cover(po.getCover())
                .tags(readStringList(po.getTagsJson()))
                .challengeLevel(po.getChallengeLevel())
                .sort(po.getSort())
                .createdTime(po.getCreateTime())
                .updatedTime(po.getUpdateTime())
                .build();
    }

    private AiPracticeRecordVO toRecordVO(AiPracticeRecordPO po, AiProjectCasePO projectCase) {
        return AiPracticeRecordVO.builder()
                .id(po.getId())
                .caseId(po.getCaseId())
                .caseName(projectCase == null ? "" : projectCase.getProjectName())
                .userId(po.getUserId())
                .userName(po.getUserName())
                .practiceType(po.getPracticeType())
                .inputText(po.getInputText())
                .fileUrl(po.getFileUrl())
                .fileName(po.getFileName())
                .answerText(po.getAnswerText())
                .note(po.getNote())
                .aiResult(readAiResult(po.getAiResultJson()))
                .score(po.getScore())
                .status(statusText(po.getStatus()))
                .createdTime(po.getCreateTime())
                .updatedTime(po.getUpdateTime())
                .build();
    }

    private AiProjectCasePO requireCase(Long caseId) {
        if (caseId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "案例ID不能为空");
        }
        AiProjectCasePO projectCase = aiExhibitRepository.selectCaseById(caseId);
        if (projectCase == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "案例不存在");
        }
        return projectCase;
    }

    private UserInfoDTO currentUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private Map<Long, AiProjectCasePO> caseMap() {
        Map<Long, AiProjectCasePO> map = new LinkedHashMap<>();
        for (AiProjectCasePO projectCase : aiExhibitRepository.selectEnabledCases()) {
            map.put(projectCase.getId(), projectCase);
        }
        return map;
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list == null ? List.of() : list.stream().filter(StringUtils::hasText).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<AiRubricItemVO> readRubricList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<AiRubricItemVO> list = objectMapper.readValue(json, new TypeReference<List<AiRubricItemVO>>() {});
            return list == null ? List.of() : list;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private AiPracticeAiResultVO readAiResult(String json) {
        if (!StringUtils.hasText(json)) {
            return AiPracticeAiResultVO.builder()
                    .title("AI 结果")
                    .summary("暂无 AI 返回结果")
                    .highlights(List.of())
                    .suggestions(List.of())
                    .nextSteps(List.of())
                    .build();
        }
        try {
            return objectMapper.readValue(json, AiPracticeAiResultVO.class);
        } catch (Exception ignored) {
            return AiPracticeAiResultVO.builder()
                    .title("AI 结果")
                    .summary(json)
                    .highlights(List.of())
                    .suggestions(List.of())
                    .nextSteps(List.of())
                    .build();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String writeMetaJson(AiProjectCasePO projectCase, AiExhibitStorageService.StoredAiFile storedFile) {
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("projectCode", projectCase.getProjectCode());
            meta.put("practiceType", projectCase.getPracticeType());
            if (storedFile != null) {
                meta.put("attachment", storedFile.objectName());
                meta.put("attachmentName", storedFile.originalName());
            }
            return objectMapper.writeValueAsString(meta);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private AiPracticeAiResultVO generateAiResult(
            AiProjectCasePO projectCase,
            String practiceType,
            String inputText,
            String answerText,
            String note,
            AiExhibitStorageService.StoredAiFile storedFile
    ) {
        String text = StringUtils.hasText(inputText) ? inputText.trim() : "";
        return switch (projectCase.getProjectCode()) {
            case "waste_sorting_assistant" -> generateWasteResult(projectCase, text, answerText, storedFile);
            case "wrong_answer_helper" -> generateWrongAnswerResult(projectCase, text, answerText);
            case "emotion_diary_analysis" -> generateEmotionResult(projectCase, text, note);
            case "plant_recognition" -> generatePlantResult(projectCase, text, storedFile);
            case "poetry_learning_assistant" -> generatePoetryResult(projectCase, text, answerText);
            default -> AiPracticeAiResultVO.builder()
                    .title(projectCase.getProjectName())
                    .summary("已完成 AI 演示结果生成。")
                    .highlights(List.of("实践类型：" + practiceType, "输入长度：" + text.length()))
                    .suggestions(List.of("后续可接入真实模型接口替换本地演示引擎。"))
                    .nextSteps(List.of("整理作品页面", "提交课堂展示", "补充反思记录"))
                    .build();
        };
    }

    private AiPracticeAiResultVO generateWasteResult(
            AiProjectCasePO projectCase,
            String inputText,
            String answerText,
            AiExhibitStorageService.StoredAiFile storedFile
    ) {
        String category = classifyWaste(inputText);
        return AiPracticeAiResultVO.builder()
                .title(projectCase.getProjectName())
                .summary("已根据输入内容生成垃圾分类演示结果，当前建议类别为：" + category + "。")
                .highlights(List.of(
                        "识别对象：" + (StringUtils.hasText(inputText) ? inputText : "上传图片中的内容"),
                        "建议分类：" + category,
                        "提交形式：" + (storedFile == null ? "文本描述" : "图片附件")
                ))
                .suggestions(List.of(
                        "可以补充更多样本图片，提高分类准确率。",
                        "可以把识别结果拆成“类别 + 理由 + 操作建议”三部分展示。"
                ))
                .nextSteps(List.of(
                        "加入不同垃圾类别的示例图。",
                        "为识别结果增加置信度和解释文本。"
                ))
                .build();
    }

    private AiPracticeAiResultVO generateWrongAnswerResult(
            AiProjectCasePO projectCase,
            String inputText,
            String answerText
    ) {
        String topic = StringUtils.hasText(inputText) ? inputText : "这道题";
        return AiPracticeAiResultVO.builder()
                .title(projectCase.getProjectName())
                .summary("已生成解题思路、知识点和相似练习建议。")
                .highlights(List.of(
                        "题目主题：" + topic,
                        "核心思路：先找已知量，再建立等式或方程，最后验证答案。",
                        "知识点：列方程、运算顺序、条件整理"
                ))
                .suggestions(List.of(
                        "把答案拆成“思路 - 公式 - 验证 - 易错点”四段。",
                        "加入 1 到 3 道同类练习题，形成巩固闭环。"
                ))
                .nextSteps(List.of(
                        "接入真实大模型后可改为分步追问式讲解。",
                        "增加学生自评区，记录不会的步骤。"
                ))
                .build();
    }

    private AiPracticeAiResultVO generateEmotionResult(
            AiProjectCasePO projectCase,
            String inputText,
            String note
    ) {
        String mood = detectMood(inputText + " " + note);
        return AiPracticeAiResultVO.builder()
                .title(projectCase.getProjectName())
                .summary("已根据日记内容生成情绪倾向分析，当前判断为：" + mood + "。")
                .highlights(List.of(
                        "情绪倾向：" + mood,
                        "建议关注：情绪触发点、时间线和支持资源",
                        "表达方式：先肯定感受，再给出可执行建议"
                ))
                .suggestions(List.of(
                        "鼓励学生用“发生了什么 - 我感受如何 - 我可以做什么”写反思。",
                        "可以加入情绪温度计和每日趋势图。"
                ))
                .nextSteps(List.of(
                        "扩展为一周情绪追踪。",
                        "加入积极心理学建议卡片。"
                ))
                .build();
    }

    private AiPracticeAiResultVO generatePlantResult(
            AiProjectCasePO projectCase,
            String inputText,
            AiExhibitStorageService.StoredAiFile storedFile
    ) {
        String clue = StringUtils.hasText(inputText) ? inputText : (storedFile == null ? "图片" : storedFile.originalName());
        return AiPracticeAiResultVO.builder()
                .title(projectCase.getProjectName())
                .summary("已完成植物识别演示，建议以“植物名称 + 科普介绍 + 观察要点”的方式展示。")
                .highlights(List.of(
                        "识别线索：" + clue,
                        "科普方向：形态特征、常见栽培环境、学习价值",
                        "展示建议：图文并茂，突出校园植物观察"
                ))
                .suggestions(List.of(
                        "给识别结果增加拉丁学名和花期信息。",
                        "可联动校园植物地图，形成实践任务。"
                ))
                .nextSteps(List.of(
                        "接入真实视觉模型识别图片。",
                        "增加学生拍照打卡和物候观察记录。"
                ))
                .build();
    }

    private AiPracticeAiResultVO generatePoetryResult(
            AiProjectCasePO projectCase,
            String inputText,
            String answerText
    ) {
        return AiPracticeAiResultVO.builder()
                .title(projectCase.getProjectName())
                .summary("已生成古诗词解读、意象分析和背诵练习建议。")
                .highlights(List.of(
                        "原诗内容：" + (StringUtils.hasText(inputText) ? inputText : "未填写"),
                        "解读方式：逐句解释、提炼意象、总结情感",
                        "练习建议：填空背诵、意象配对、改写仿写"
                ))
                .suggestions(List.of(
                        "可以把知识点拆成“字词 - 句意 - 意象 - 情感”四层。",
                        "加入朗读录音与自动评分，提升体验。"
                ))
                .nextSteps(List.of(
                        "接入语音识别后支持朗读提交。",
                        "增加诗词知识图谱与拓展阅读。"
                ))
                .build();
    }

    private Integer calculateScore(
            AiProjectCasePO projectCase,
            String inputText,
            String answerText,
            AiExhibitStorageService.StoredAiFile storedFile,
            AiPracticeAiResultVO aiResult
    ) {
        int score = 60;
        if (StringUtils.hasText(inputText)) {
            score += 12;
        }
        if (StringUtils.hasText(answerText)) {
            score += 8;
        }
        if (storedFile != null) {
            score += 10;
        }
        if (aiResult != null && aiResult.getSuggestions() != null && !aiResult.getSuggestions().isEmpty()) {
            score += 5;
        }
        if (projectCase.getChallengeLevel() != null) {
            score += Math.min(projectCase.getChallengeLevel() * 2, 6);
        }
        return Math.min(score, 100);
    }

    private String classifyWaste(String inputText) {
        String text = inputText == null ? "" : inputText;
        if (containsAny(text, "电池", "灯管", "药", "过期", "油漆")) {
            return "有害垃圾";
        }
        if (containsAny(text, "剩饭", "果皮", "菜叶", "骨头", "厨余")) {
            return "厨余垃圾";
        }
        if (containsAny(text, "纸", "瓶", "塑料", "金属", "衣服", "玻璃")) {
            return "可回收物";
        }
        return "其他垃圾";
    }

    private String detectMood(String text) {
        if (containsAny(text, "开心", "高兴", "期待", "顺利", "喜欢")) {
            return "积极";
        }
        if (containsAny(text, "难过", "焦虑", "压力", "烦", "生气", "失落")) {
            return "需要关注";
        }
        return "平稳";
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String statusText(Integer status) {
        return Objects.equals(status, RECORD_STATUS_SUBMITTED) ? "submitted" : "draft";
    }
}
