package com.edu.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.edu.pojo.po.EduGovNewsCategoryPO;
import com.edu.pojo.po.EduGovNewsPO;

import java.util.List;

public interface GovNewsRepository {
    List<EduGovNewsCategoryPO> selectPublicCategories();

    List<EduGovNewsCategoryPO> selectAdminCategories();

    EduGovNewsCategoryPO selectCategoryById(Long categoryId);

    int insertCategory(EduGovNewsCategoryPO category);

    int updateCategory(EduGovNewsCategoryPO category);

    IPage<EduGovNewsPO> selectPublicNewsPage(long pageNum, long pageSize, Long categoryId, String keyword);

    IPage<EduGovNewsPO> selectAdminNewsPage(long pageNum, long pageSize, Long categoryId, String keyword, Integer status);

    EduGovNewsPO selectPublicNewsById(Long newsId);

    EduGovNewsPO selectAdminNewsById(Long newsId);

    int insertNews(EduGovNewsPO news);

    int updateNews(EduGovNewsPO news);
}
