package com.edu.service.impl.learning;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.BaseException;
import com.edu.mapper.learning.LearningWrongBookItemMapper;
import com.edu.mapper.learning.LearningWrongBookMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.learning.LearningWrongBookNameRequest;
import com.edu.pojo.dto.learning.LearningWrongBookQuestionRequest;
import com.edu.pojo.po.learning.LearningWrongBookItemPO;
import com.edu.pojo.po.learning.LearningWrongBookPO;
import com.edu.pojo.vo.learning.LearningPracticeEvidenceVO;
import com.edu.pojo.vo.learning.LearningStudentGrowthVO;
import com.edu.pojo.vo.learning.LearningWrongBookVO;
import com.edu.service.learning.LearningAnalysisService;
import com.edu.service.learning.LearningWrongBookService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningWrongBookServiceImpl implements LearningWrongBookService {
    private static final int MAX_BOOKS = 20;
    private static final String ROLE_STUDENT = "STUDENT";

    private final LearningWrongBookMapper bookMapper;
    private final LearningWrongBookItemMapper itemMapper;
    private final LearningAnalysisService learningAnalysisService;

    @Override
    public List<LearningWrongBookVO> listStudentBooks() {
        UserInfoDTO student = requireStudent();
        List<LearningWrongBookPO> books = bookMapper.selectList(new LambdaQueryWrapper<LearningWrongBookPO>()
                .eq(LearningWrongBookPO::getStudentId, student.getUserId())
                .orderByAsc(LearningWrongBookPO::getSortOrder)
                .orderByAsc(LearningWrongBookPO::getId));
        if (books.isEmpty()) return List.of();

        Map<Long, List<LearningWrongBookItemPO>> itemsByBook = itemMapper.selectList(
                        new LambdaQueryWrapper<LearningWrongBookItemPO>()
                                .eq(LearningWrongBookItemPO::getStudentId, student.getUserId())
                                .in(LearningWrongBookItemPO::getBookId,
                                        books.stream().map(LearningWrongBookPO::getId).toList())
                                .orderByDesc(LearningWrongBookItemPO::getCreatedAt)
                                .orderByDesc(LearningWrongBookItemPO::getId))
                .stream()
                .collect(Collectors.groupingBy(LearningWrongBookItemPO::getBookId));
        return books.stream()
                .map(book -> toView(book, itemsByBook.getOrDefault(book.getId(), Collections.emptyList())))
                .toList();
    }

    @Override
    @Transactional
    public LearningWrongBookVO createBook(LearningWrongBookNameRequest request) {
        UserInfoDTO student = requireStudent();
        Long count = bookMapper.selectCount(new LambdaQueryWrapper<LearningWrongBookPO>()
                .eq(LearningWrongBookPO::getStudentId, student.getUserId()));
        if (count != null && count >= MAX_BOOKS) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "最多创建20个错题本");
        }
        String name = normalizedName(request.getName());
        ensureNameAvailable(student.getUserId(), null, name);
        LocalDateTime now = LocalDateTime.now();
        LearningWrongBookPO book = LearningWrongBookPO.builder()
                .studentId(student.getUserId())
                .name(name)
                .sortOrder(count == null ? 0 : count.intValue())
                .createdAt(now)
                .updatedAt(now)
                .build();
        bookMapper.insert(book);
        return toView(book, List.of());
    }

    @Override
    @Transactional
    public LearningWrongBookVO renameBook(Long bookId, LearningWrongBookNameRequest request) {
        UserInfoDTO student = requireStudent();
        LearningWrongBookPO book = requireOwnedBook(bookId, student.getUserId());
        String name = normalizedName(request.getName());
        ensureNameAvailable(student.getUserId(), bookId, name);
        book.setName(name);
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
        return toView(book, ownedItems(bookId, student.getUserId()));
    }

    @Override
    @Transactional
    public void deleteBook(Long bookId) {
        UserInfoDTO student = requireStudent();
        requireOwnedBook(bookId, student.getUserId());
        itemMapper.delete(new LambdaQueryWrapper<LearningWrongBookItemPO>()
                .eq(LearningWrongBookItemPO::getBookId, bookId)
                .eq(LearningWrongBookItemPO::getStudentId, student.getUserId()));
        bookMapper.deleteById(bookId);
    }

    @Override
    @Transactional
    public LearningWrongBookVO addQuestion(Long bookId, LearningWrongBookQuestionRequest request) {
        UserInfoDTO student = requireStudent();
        LearningWrongBookPO book = requireOwnedBook(bookId, student.getUserId());
        Long existing = itemMapper.selectCount(new LambdaQueryWrapper<LearningWrongBookItemPO>()
                .eq(LearningWrongBookItemPO::getBookId, bookId)
                .eq(LearningWrongBookItemPO::getPracticeId, request.getPracticeId())
                .eq(LearningWrongBookItemPO::getQuestionId, request.getQuestionId()));
        if (existing == null || existing == 0) {
            LearningPracticeEvidenceVO.WrongQuestion question = currentWrongQuestions().stream()
                    .filter(item -> request.getPracticeId().equals(item.getPracticeId())
                            && request.getQuestionId().equals(item.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new BaseException(HttpStatus.BAD_REQUEST, "该题不在当前真实错题记录中"));
            itemMapper.insert(LearningWrongBookItemPO.builder()
                    .bookId(bookId)
                    .studentId(student.getUserId())
                    .practiceId(question.getPracticeId())
                    .questionId(question.getQuestionId())
                    .practiceTitle(question.getPracticeTitle())
                    .courseName(question.getCourseName())
                    .questionContent(question.getContent())
                    .questionScore(question.getScore())
                    .awardedScore(question.getAwardedScore())
                    .referenceAnswer(question.getReferenceAnswer())
                    .explanation(question.getExplanation())
                    .createdAt(LocalDateTime.now())
                    .build());
            book.setUpdatedAt(LocalDateTime.now());
            bookMapper.updateById(book);
        }
        return toView(book, ownedItems(bookId, student.getUserId()));
    }

    @Override
    @Transactional
    public void removeQuestion(Long bookId, Long practiceId, Long questionId) {
        UserInfoDTO student = requireStudent();
        LearningWrongBookPO book = requireOwnedBook(bookId, student.getUserId());
        itemMapper.delete(new LambdaQueryWrapper<LearningWrongBookItemPO>()
                .eq(LearningWrongBookItemPO::getBookId, bookId)
                .eq(LearningWrongBookItemPO::getStudentId, student.getUserId())
                .eq(LearningWrongBookItemPO::getPracticeId, practiceId)
                .eq(LearningWrongBookItemPO::getQuestionId, questionId));
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    private List<LearningPracticeEvidenceVO.WrongQuestion> currentWrongQuestions() {
        LearningStudentGrowthVO growth = learningAnalysisService.getStudentGrowthOverview();
        if (growth == null || growth.getPracticeEvidence() == null
                || growth.getPracticeEvidence().getWrongQuestions() == null) {
            return List.of();
        }
        return growth.getPracticeEvidence().getWrongQuestions();
    }

    private LearningWrongBookPO requireOwnedBook(Long bookId, Long studentId) {
        LearningWrongBookPO book = bookMapper.selectOne(new LambdaQueryWrapper<LearningWrongBookPO>()
                .eq(LearningWrongBookPO::getId, bookId)
                .eq(LearningWrongBookPO::getStudentId, studentId));
        if (book == null) throw new BaseException(HttpStatus.NOT_FOUND, "错题本不存在");
        return book;
    }

    private List<LearningWrongBookItemPO> ownedItems(Long bookId, Long studentId) {
        return itemMapper.selectList(new LambdaQueryWrapper<LearningWrongBookItemPO>()
                .eq(LearningWrongBookItemPO::getBookId, bookId)
                .eq(LearningWrongBookItemPO::getStudentId, studentId)
                .orderByDesc(LearningWrongBookItemPO::getCreatedAt)
                .orderByDesc(LearningWrongBookItemPO::getId));
    }

    private void ensureNameAvailable(Long studentId, Long excludedBookId, String name) {
        LambdaQueryWrapper<LearningWrongBookPO> query = new LambdaQueryWrapper<LearningWrongBookPO>()
                .eq(LearningWrongBookPO::getStudentId, studentId)
                .eq(LearningWrongBookPO::getName, name);
        if (excludedBookId != null) query.ne(LearningWrongBookPO::getId, excludedBookId);
        if (bookMapper.selectCount(query) > 0) {
            throw new BaseException(HttpStatus.CONFLICT, "已经有同名错题本了");
        }
    }

    private String normalizedName(String name) {
        return name == null ? "" : name.trim();
    }

    private LearningWrongBookVO toView(LearningWrongBookPO book, List<LearningWrongBookItemPO> items) {
        List<LearningWrongBookVO.QuestionItem> questions = items.stream()
                .map(item -> LearningWrongBookVO.QuestionItem.builder()
                        .practiceId(item.getPracticeId())
                        .questionId(item.getQuestionId())
                        .practiceTitle(item.getPracticeTitle())
                        .courseName(item.getCourseName())
                        .content(item.getQuestionContent())
                        .score(item.getQuestionScore())
                        .awardedScore(item.getAwardedScore())
                        .referenceAnswer(item.getReferenceAnswer())
                        .explanation(item.getExplanation())
                        .build())
                .toList();
        return LearningWrongBookVO.builder()
                .id(book.getId())
                .name(book.getName())
                .questionCount(questions.size())
                .updatedAt(book.getUpdatedAt())
                .questions(questions)
                .build();
    }

    private UserInfoDTO requireStudent() {
        UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class);
        if (user == null || user.getUserId() == null || !ROLE_STUDENT.equalsIgnoreCase(user.getRoleCode())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅学生可管理自己的错题本");
        }
        return user;
    }
}
