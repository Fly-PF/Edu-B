package com.edu.service;

import com.edu.pojo.dto.gov.GovMaterialCategorySaveRequest;
import com.edu.pojo.vo.gov.GovMaterialCategoryVO;

import java.util.List;

public interface GovMaterialCategoryService {
    List<GovMaterialCategoryVO> listEnabledCategories();

    List<GovMaterialCategoryVO> listAllCategories();

    GovMaterialCategoryVO createCategory(GovMaterialCategorySaveRequest request);

    GovMaterialCategoryVO updateCategory(Long categoryId, GovMaterialCategorySaveRequest request);

    void deleteCategory(Long categoryId);
}
