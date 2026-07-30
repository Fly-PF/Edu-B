package com.edu.service;

import com.edu.pojo.dto.course.CourseCategorySaveRequest;
import com.edu.pojo.vo.course.CourseCategoryVO;

import java.util.List;

public interface CourseCategoryService {
    List<CourseCategoryVO> listPublicCategories();

    List<String> listAvailableTags();

    CourseCategoryVO createCategory(CourseCategorySaveRequest request);

    CourseCategoryVO updateCategory(Long categoryId, CourseCategorySaveRequest request);

    void deleteCategory(Long categoryId);
}
