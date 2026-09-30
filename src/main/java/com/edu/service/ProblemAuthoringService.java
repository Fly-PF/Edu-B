package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.problem.ProblemAuthoringRequest;
import com.edu.pojo.dto.problem.ProblemCategorySaveRequest;
import com.edu.pojo.vo.problem.ProblemDetailVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ProblemAuthoringService {
    List<Map<String, Object>> subjects();
    List<Map<String, Object>> categories(Long subjectId, boolean includeDisabled);
    Long createProblem(ProblemAuthoringRequest request, List<MultipartFile> images);
    void updateProblem(Long problemId, ProblemAuthoringRequest request, List<MultipartFile> images);
    void updateOwnProblem(Long problemId, ProblemAuthoringRequest request, List<MultipartFile> images);
    PageResult<Map<String, Object>> problems(Integer pageNum, Integer pageSize);
    PageResult<Map<String, Object>> ownProblems(Integer pageNum, Integer pageSize);
    PageResult<Map<String, Object>> managedCategories(Integer pageNum, Integer pageSize);
    ProblemDetailVO problem(Long problemId);
    ProblemDetailVO ownProblem(Long problemId);
    ResponseEntity<byte[]> getProblemImage(Long problemId, Long imageId);
    void updateProblemStatus(Long problemId, Integer status);
    void updateOwnProblemStatus(Long problemId, Integer status);
    void deleteProblem(Long problemId);
    void deleteOwnProblem(Long problemId);
    Long createCategory(ProblemCategorySaveRequest request);
    void updateCategoryStatus(Long categoryId, Integer status);
    void deleteCategory(Long categoryId);
}
