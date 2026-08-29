package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.common.PageQuery;
import com.edu.common.PageResult;
import com.edu.exception.BaseException;
import com.edu.mapper.GovQuestionMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovQuestionContentDTO;
import com.edu.pojo.dto.gov.GovQuestionSaveRequest;
import com.edu.pojo.po.gov.GovQuestionPO;
import com.edu.pojo.vo.gov.GovQuestionAdminVO;
import com.edu.pojo.vo.gov.GovQuestionImportResultVO;
import com.edu.service.GovQuestionAdminService;
import com.edu.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GovQuestionAdminServiceImpl implements GovQuestionAdminService {
    private static final Set<String> ALLOWED_SUBJECTS = Set.of(
            "政治理论",
            "常识判断",
            "语言理解与表达",
            "数量关系",
            "判断推理",
            "资料分析"
    );
    private static final Set<String> ALLOWED_QUESTION_TYPES = Set.of("SINGLE", "MULTIPLE");
    private static final Set<String> ALLOWED_SOURCE_TYPES = Set.of("REAL", "SIMULATION");
    private static final int MAX_IMPORT_COUNT = 1000;
    private static final int NOT_DELETED = 0;

    private final GovQuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<GovQuestionAdminVO> pageQuestions(
            String subject,
            String questionType,
            Integer status,
            String keyword,
            Integer pageNum,
            Integer pageSize
    ) {
        PageQuery pageQuery = PageQuery.of(pageNum, pageSize);
        LambdaQueryWrapper<GovQuestionPO> wrapper = new LambdaQueryWrapper<GovQuestionPO>()
                .eq(GovQuestionPO::getDeleted, NOT_DELETED)
                .eq(StringUtils.hasText(subject), GovQuestionPO::getSubject, subject == null ? null : subject.trim())
                .eq(StringUtils.hasText(questionType), GovQuestionPO::getQuestionType,
                        questionType == null ? null : questionType.trim().toUpperCase())
                .eq(status != null, GovQuestionPO::getStatus, status)
                .like(StringUtils.hasText(keyword), GovQuestionPO::getContentJson, keyword == null ? null : keyword.trim())
                .orderByDesc(GovQuestionPO::getUpdateTime)
                .orderByDesc(GovQuestionPO::getId);

        IPage<GovQuestionPO> page = questionMapper.selectPage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()),
                wrapper
        );
        return PageResult.of(
                page.getTotal(),
                pageQuery,
                page.getRecords().stream().map(this::toView).toList()
        );
    }

    @Override
    public GovQuestionAdminVO getQuestion(Long questionId) {
        return toView(requireQuestion(questionId));
    }

    @Override
    @Transactional
    public GovQuestionAdminVO createQuestion(GovQuestionSaveRequest request) {
        UserInfoDTO user = requireUser();
        LocalDateTime now = LocalDateTime.now();
        GovQuestionContentDTO content = normalizeContent(request);
        GovQuestionPO question = GovQuestionPO.builder()
                .subject(normalizeSubject(request.getSubject()))
                .questionType(normalizeQuestionType(request.getQuestionType()))
                .difficulty(request.getDifficulty())
                .examYear(request.getExamYear())
                .sourceType(normalizeSourceType(request.getSourceType()))
                .contentJson(writeJson(content))
                .status(normalizeStatus(request.getStatus()))
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(NOT_DELETED)
                .build();
        questionMapper.insert(question);
        if (question.getId() == null) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "题目创建失败");
        }
        return toView(question);
    }

    @Override
    @Transactional
    public GovQuestionAdminVO updateQuestion(Long questionId, GovQuestionSaveRequest request) {
        UserInfoDTO user = requireUser();
        GovQuestionPO question = requireQuestion(questionId);
        GovQuestionContentDTO content = normalizeContent(request);
        question.setSubject(normalizeSubject(request.getSubject()));
        question.setQuestionType(normalizeQuestionType(request.getQuestionType()));
        question.setDifficulty(request.getDifficulty());
        question.setExamYear(request.getExamYear());
        question.setSourceType(normalizeSourceType(request.getSourceType()));
        question.setContentJson(writeJson(content));
        question.setStatus(normalizeStatus(request.getStatus()));
        question.setUpdateBy(user.getUserId());
        question.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(question);
        return toView(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        UserInfoDTO user = requireUser();
        GovQuestionPO question = requireQuestion(questionId);
        question.setDeleted(1);
        question.setUpdateBy(user.getUserId());
        question.setUpdateTime(LocalDateTime.now());
        questionMapper.updateById(question);
    }

    @Override
    public GovQuestionImportResultVO importQuestions(List<GovQuestionSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "导入题目不能为空");
        }
        if (requests.size() > MAX_IMPORT_COUNT) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "单次最多导入 " + MAX_IMPORT_COUNT + " 道题");
        }

        UserInfoDTO user = requireUser();
        LocalDateTime now = LocalDateTime.now();
        List<GovQuestionImportResultVO.GovQuestionImportErrorVO> errors = new ArrayList<>();
        int successCount = 0;

        for (int index = 0; index < requests.size(); index++) {
            GovQuestionSaveRequest request = requests.get(index);
            try {
                GovQuestionContentDTO content = normalizeContent(request);
                GovQuestionPO question = GovQuestionPO.builder()
                        .subject(normalizeSubject(request.getSubject()))
                        .questionType(normalizeQuestionType(request.getQuestionType()))
                        .difficulty(request.getDifficulty())
                        .examYear(request.getExamYear())
                        .sourceType(normalizeSourceType(request.getSourceType()))
                        .contentJson(writeJson(content))
                        .status(normalizeStatus(request.getStatus()))
                        .createBy(user.getUserId())
                        .updateBy(user.getUserId())
                        .createTime(now)
                        .updateTime(now)
                        .deleted(NOT_DELETED)
                        .build();
                questionMapper.insert(question);
                if (question.getId() == null) {
                    throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "题目创建失败");
                }
                successCount++;
            } catch (Exception exception) {
                errors.add(GovQuestionImportResultVO.GovQuestionImportErrorVO.builder()
                        .index(index + 1)
                        .reason(exception.getMessage() == null ? "题目格式错误" : exception.getMessage())
                        .build());
            }
        }

        return GovQuestionImportResultVO.builder()
                .totalCount(requests.size())
                .successCount(successCount)
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    private GovQuestionContentDTO normalizeContent(GovQuestionSaveRequest request) {
        if (request == null || request.getContent() == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "题目内容不能为空");
        }

        GovQuestionContentDTO content = request.getContent();
        if (!StringUtils.hasText(content.getStem())) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "题干不能为空");
        }

        List<GovQuestionContentDTO.Option> options = content.getOptions() == null
                ? List.of()
                : content.getOptions().stream()
                .filter(option -> option != null && StringUtils.hasText(option.getKey()) && StringUtils.hasText(option.getContent()))
                .map(option -> GovQuestionContentDTO.Option.builder()
                        .key(option.getKey().trim().toUpperCase())
                        .content(option.getContent().trim())
                        .build())
                .toList();
        if (options.size() < 2) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "每道题至少需要两个有效选项");
        }

        Set<String> optionKeys = options.stream()
                .map(GovQuestionContentDTO.Option::getKey)
                .collect(Collectors.toSet());
        if (optionKeys.size() != options.size()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "选项标识不能重复");
        }

        List<String> answer = content.getAnswer() == null
                ? List.of()
                : content.getAnswer().stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase())
                .distinct()
                .sorted()
                .toList();
        if (answer.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "正确答案不能为空");
        }
        if (!optionKeys.containsAll(answer)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "正确答案必须在选项标识中");
        }
        if ("SINGLE".equalsIgnoreCase(request.getQuestionType()) && answer.size() != 1) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "单选题只能有一个正确答案");
        }

        List<String> tags = content.getTags() == null
                ? List.of()
                : content.getTags().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return GovQuestionContentDTO.builder()
                .stem(content.getStem().trim())
                .material(StringUtils.hasText(content.getMaterial()) ? content.getMaterial().trim() : null)
                .options(options)
                .answer(answer)
                .analysis(StringUtils.hasText(content.getAnalysis()) ? content.getAnalysis().trim() : null)
                .tags(tags)
                .build();
    }

    private GovQuestionPO requireQuestion(Long questionId) {
        GovQuestionPO question = questionMapper.selectOne(new LambdaQueryWrapper<GovQuestionPO>()
                .eq(GovQuestionPO::getId, questionId)
                .eq(GovQuestionPO::getDeleted, NOT_DELETED));
        if (question == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private UserInfoDTO requireUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class);
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private String normalizeSubject(String subject) {
        if (!StringUtils.hasText(subject)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "科目不能为空");
        }
        String value = subject.trim();
        if (!ALLOWED_SUBJECTS.contains(value)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的科目");
        }
        return value;
    }

    private String normalizeQuestionType(String questionType) {
        if (!StringUtils.hasText(questionType)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "题型不能为空");
        }
        String value = questionType.trim().toUpperCase();
        if (!ALLOWED_QUESTION_TYPES.contains(value)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的题型");
        }
        return value;
    }

    private String normalizeSourceType(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "题目来源不能为空");
        }
        String value = sourceType.trim().toUpperCase();
        if (!ALLOWED_SOURCE_TYPES.contains(value)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的题目来源");
        }
        return value;
    }

    private int normalizeStatus(Integer status) {
        if (status == null || status == 0) {
            return 0;
        }
        if (status == 1 || status == 2) {
            return status;
        }
        throw new BaseException(HttpStatus.BAD_REQUEST, "不支持的题目状态");
    }

    private String writeJson(GovQuestionContentDTO content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception exception) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "题目内容序列化失败");
        }
    }

    private GovQuestionAdminVO toView(GovQuestionPO question) {
        try {
            return GovQuestionAdminVO.builder()
                    .id(question.getId())
                    .subject(question.getSubject())
                    .questionType(question.getQuestionType())
                    .difficulty(question.getDifficulty())
                    .examYear(question.getExamYear())
                    .sourceType(question.getSourceType())
                    .content(objectMapper.readValue(question.getContentJson(), GovQuestionContentDTO.class))
                    .status(question.getStatus())
                    .createTime(question.getCreateTime())
                    .updateTime(question.getUpdateTime())
                    .build();
        } catch (Exception exception) {
            log.warn("Failed to parse gov question {} content", question.getId(), exception);
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "题目内容格式错误");
        }
    }
}

