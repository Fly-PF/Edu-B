package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.problem.ProblemExplainRequest;
import com.edu.pojo.dto.problem.ProblemSessionCreateRequest;
import com.edu.pojo.vo.problem.ProblemCategoryVO;
import com.edu.pojo.vo.problem.ProblemDetailVO;
import com.edu.pojo.vo.problem.ProblemExplanationVO;
import com.edu.pojo.vo.problem.ProblemListItemVO;
import com.edu.pojo.vo.problem.ProblemSubjectVO;
import com.edu.pojo.vo.problem.ProblemSessionVO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProblemSolvingService {
    List<ProblemSubjectVO> listSubjects();
    List<ProblemCategoryVO> listCategories(Long subjectId);
    PageResult<ProblemListItemVO> pageProblems(Long subjectId, Long categoryId, String difficulty, String keyword, Integer pageNum, Integer pageSize);
    ProblemDetailVO getProblem(Long problemId);
    ResponseEntity<byte[]> getProblemImage(Long problemId, Long imageId);
    ProblemExplanationVO explain(Long problemId, ProblemExplainRequest request);
    ProblemSessionVO createSession(ProblemSessionCreateRequest request);
}
