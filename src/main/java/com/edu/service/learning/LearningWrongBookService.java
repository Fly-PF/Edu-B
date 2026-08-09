package com.edu.service.learning;

import com.edu.pojo.dto.learning.LearningWrongBookNameRequest;
import com.edu.pojo.dto.learning.LearningWrongBookQuestionRequest;
import com.edu.pojo.vo.learning.LearningWrongBookVO;

import java.util.List;

public interface LearningWrongBookService {
    List<LearningWrongBookVO> listStudentBooks();

    LearningWrongBookVO createBook(LearningWrongBookNameRequest request);

    LearningWrongBookVO renameBook(Long bookId, LearningWrongBookNameRequest request);

    void deleteBook(Long bookId);

    LearningWrongBookVO addQuestion(Long bookId, LearningWrongBookQuestionRequest request);

    void removeQuestion(Long bookId, Long practiceId, Long questionId);
}
