package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.BaseException;
import com.edu.mapper.EduGovMaterialCategoryMapper;
import com.edu.mapper.EduGovMaterialMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovMaterialCategorySaveRequest;
import com.edu.pojo.po.EduGovMaterialCategoryPO;
import com.edu.pojo.po.EduGovMaterialPO;
import com.edu.pojo.vo.gov.GovMaterialCategoryVO;
import com.edu.service.GovMaterialCategoryService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GovMaterialCategoryServiceImpl implements GovMaterialCategoryService {
    private final EduGovMaterialCategoryMapper categoryMapper;
    private final EduGovMaterialMapper materialMapper;

    @Override
    public List<GovMaterialCategoryVO> listEnabledCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EduGovMaterialCategoryPO>()
                        .eq(EduGovMaterialCategoryPO::getDeleted, 0)
                        .eq(EduGovMaterialCategoryPO::getStatus, 1)
                        .orderByDesc(EduGovMaterialCategoryPO::getSortOrder)
                        .orderByDesc(EduGovMaterialCategoryPO::getCreateTime))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public List<GovMaterialCategoryVO> listAllCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EduGovMaterialCategoryPO>()
                        .eq(EduGovMaterialCategoryPO::getDeleted, 0)
                        .orderByDesc(EduGovMaterialCategoryPO::getSortOrder)
                        .orderByDesc(EduGovMaterialCategoryPO::getCreateTime))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional
    public GovMaterialCategoryVO createCategory(GovMaterialCategorySaveRequest request) {
        UserInfoDTO user = requireUser();
        validateNameUnique(request.getName().trim(), null);
        EduGovMaterialCategoryPO category = EduGovMaterialCategoryPO.builder()
                .name(request.getName().trim())
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .status(request.getStatus() == null ? 1 : normalizeStatus(request.getStatus()))
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();
        categoryMapper.insert(category);
        return toView(category);
    }

    @Override
    @Transactional
    public GovMaterialCategoryVO updateCategory(Long categoryId, GovMaterialCategorySaveRequest request) {
        UserInfoDTO user = requireUser();
        EduGovMaterialCategoryPO category = requireCategory(categoryId);
        validateNameUnique(request.getName().trim(), categoryId);
        category.setName(request.getName().trim());
        category.setSortOrder(request.getSortOrder() == null ? category.getSortOrder() : request.getSortOrder());
        if (request.getStatus() != null) {
            category.setStatus(normalizeStatus(request.getStatus()));
        }
        category.setUpdateBy(user.getUserId());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
        return toView(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        UserInfoDTO user = requireUser();
        EduGovMaterialCategoryPO category = requireCategory(categoryId);
        Long materialCount = materialMapper.selectCount(new LambdaQueryWrapper<EduGovMaterialPO>()
                .eq(EduGovMaterialPO::getCategoryId, categoryId)
                .eq(EduGovMaterialPO::getDeleted, 0));
        if (materialCount != null && materialCount > 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "该分类下存在资料，无法删除");
        }
        category.setDeleted(1);
        category.setUpdateBy(user.getUserId());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    private GovMaterialCategoryVO toView(EduGovMaterialCategoryPO category) {
        return GovMaterialCategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .createTime(category.getCreateTime())
                .updateTime(category.getUpdateTime())
                .build();
    }

    private EduGovMaterialCategoryPO requireCategory(Long categoryId) {
        EduGovMaterialCategoryPO category = categoryMapper.selectOne(new LambdaQueryWrapper<EduGovMaterialCategoryPO>()
                .eq(EduGovMaterialCategoryPO::getId, categoryId)
                .eq(EduGovMaterialCategoryPO::getDeleted, 0));
        if (category == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private UserInfoDTO requireUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class);
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private void validateNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<EduGovMaterialCategoryPO> wrapper = new LambdaQueryWrapper<EduGovMaterialCategoryPO>()
                .eq(EduGovMaterialCategoryPO::getDeleted, 0)
                .eq(EduGovMaterialCategoryPO::getName, name);
        if (excludeId != null) {
            wrapper.ne(EduGovMaterialCategoryPO::getId, excludeId);
        }
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "分类名称已存在");
        }
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }
}
