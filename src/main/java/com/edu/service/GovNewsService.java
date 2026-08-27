package com.edu.service;

import com.edu.common.PageResult;
import com.edu.pojo.dto.gov.GovNewsCategoryCreateRequest;
import com.edu.pojo.dto.gov.GovNewsCategoryUpdateRequest;
import com.edu.pojo.dto.gov.GovNewsCreateRequest;
import com.edu.pojo.dto.gov.GovNewsUpdateRequest;
import com.edu.pojo.vo.gov.GovNewsCategoryVO;
import com.edu.pojo.vo.gov.GovNewsDetailVO;
import com.edu.pojo.vo.gov.GovNewsListItemVO;

import java.util.List;

public interface GovNewsService {
    List<GovNewsCategoryVO> listPublicCategories();

    PageResult<GovNewsListItemVO> pagePublicNews(Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    GovNewsDetailVO getPublicNews(Long newsId);

    List<GovNewsCategoryVO> listAdminCategories();

    GovNewsCategoryVO createCategory(GovNewsCategoryCreateRequest request);

    GovNewsCategoryVO updateCategory(Long categoryId, GovNewsCategoryUpdateRequest request);

    GovNewsCategoryVO updateCategoryStatus(Long categoryId, Integer status);

    PageResult<GovNewsListItemVO> pageAdminNews(
            Long categoryId,
            String keyword,
            Integer status,
            Integer pageNum,
            Integer pageSize
    );

    GovNewsDetailVO getAdminNews(Long newsId);

    GovNewsDetailVO createNews(GovNewsCreateRequest request);

    GovNewsDetailVO updateNews(Long newsId, GovNewsUpdateRequest request);

    GovNewsDetailVO publishNews(Long newsId);

    GovNewsDetailVO offlineNews(Long newsId);
}
