package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.edu.exception.BaseException;
import com.edu.mapper.EduCourseCategoryMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.course.CourseCategorySaveRequest;
import com.edu.pojo.po.EduCourseCategoryPO;
import com.edu.pojo.vo.course.CourseCategoryVO;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.service.CourseCategoryService;
import com.edu.service.CourseService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseCategoryServiceImpl implements CourseCategoryService {
    private final EduCourseCategoryMapper categoryMapper;
    private final CourseService courseService;
    private final ObjectMapper objectMapper;

    @Override
    public List<CourseCategoryVO> listPublicCategories() {
        List<CourseVO> courses = sortBySeries(courseService.listPublicCourses(null, null, null, null, null, false));
        return categoryMapper.selectList(new LambdaQueryWrapper<EduCourseCategoryPO>()
                        .eq(EduCourseCategoryPO::getDeleted, 0)
                        .orderByAsc(EduCourseCategoryPO::getSortOrder)
                        .orderByAsc(EduCourseCategoryPO::getId))
                .stream()
                .map(category -> toView(category, courses))
                .toList();
    }

    @Override
    public List<String> listAvailableTags() {
        return courseService.listPublicCourses(null, null, null, null, null, false).stream()
                .flatMap(course -> course.getTags().stream())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .sorted(String::compareTo)
                .toList();
    }

    @Override
    @Transactional
    public CourseCategoryVO createCategory(CourseCategorySaveRequest request) {
        UserInfoDTO user = requireUser();
        validateAvailableTags(request.getTags());
        EduCourseCategoryPO category = EduCourseCategoryPO.builder()
                .name(request.getName().trim())
                .sortOrder(request.getSortOrder() == null ? nextSortOrder() : request.getSortOrder())
                .tagsJson(writeTags(request.getTags()))
                .matchAll(Boolean.TRUE.equals(request.getMatchAll()) ? 1 : 0)
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();
        categoryMapper.insert(category);
        return toView(category, sortBySeries(courseService.listPublicCourses(null, null, null, null, null, false)));
    }

    @Override
    @Transactional
    public CourseCategoryVO updateCategory(Long categoryId, CourseCategorySaveRequest request) {
        UserInfoDTO user = requireUser();
        EduCourseCategoryPO category = requireCategory(categoryId);
        validateAvailableTags(request.getTags());
        category.setName(request.getName().trim());
        category.setSortOrder(request.getSortOrder() == null ? category.getSortOrder() : request.getSortOrder());
        category.setTagsJson(writeTags(request.getTags()));
        category.setMatchAll(Boolean.TRUE.equals(request.getMatchAll()) ? 1 : 0);
        category.setUpdateBy(user.getUserId());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
        return toView(category, sortBySeries(courseService.listPublicCourses(null, null, null, null, null, false)));
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        EduCourseCategoryPO category = requireCategory(categoryId);
        category.setDeleted(1);
        category.setUpdateBy(requireUser().getUserId());
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    private CourseCategoryVO toView(EduCourseCategoryPO category, List<CourseVO> courses) {
        List<String> tags = readTags(category.getTagsJson());
        boolean matchAll = Objects.equals(category.getMatchAll(), 1);
        List<CourseVO> matches = courses.stream()
                .filter(course -> matchesTags(course.getTags(), tags, matchAll))
                .toList();
        return CourseCategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .tags(tags)
                .matchAll(matchAll)
                .courses(matches.stream().limit(4).toList())
                .totalCourses((long) matches.size())
                .build();
    }

    private boolean matchesTags(List<String> courseTags, List<String> selectedTags, boolean matchAll) {
        if (selectedTags.isEmpty()) return false;
        List<String> normalized = courseTags == null ? List.of() : courseTags;
        return matchAll
                ? selectedTags.stream().allMatch(normalized::contains)
                : selectedTags.stream().anyMatch(normalized::contains);
    }

    private List<CourseVO> sortBySeries(List<CourseVO> courses) {
        return courses.stream().sorted(Comparator
                .comparing((CourseVO item) -> !StringUtils.hasText(item.getSeriesName()))
                .thenComparing(item -> StringUtils.hasText(item.getSeriesName()) ? item.getSeriesName() : "", String::compareTo)
                .thenComparing(item -> item.getSeriesOrder() == null ? Integer.MAX_VALUE : item.getSeriesOrder())
                .thenComparing(CourseVO::getUpdatedTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CourseVO::getId, Comparator.reverseOrder()))
                .toList();
    }

    private EduCourseCategoryPO requireCategory(Long categoryId) {
        EduCourseCategoryPO category = categoryMapper.selectOne(new LambdaQueryWrapper<EduCourseCategoryPO>()
                .eq(EduCourseCategoryPO::getId, categoryId)
                .eq(EduCourseCategoryPO::getDeleted, 0));
        if (category == null) throw new BaseException(HttpStatus.NOT_FOUND, "分类不存在");
        return category;
    }

    private UserInfoDTO requireUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class);
        if (user == null || user.getUserId() == null) throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        return user;
    }

    private int nextSortOrder() {
        return categoryMapper.selectList(new LambdaQueryWrapper<EduCourseCategoryPO>()
                        .eq(EduCourseCategoryPO::getDeleted, 0)
                        .orderByDesc(EduCourseCategoryPO::getSortOrder)
                        .last("LIMIT 1"))
                .stream().findFirst().map(EduCourseCategoryPO::getSortOrder).orElse(-1) + 1;
    }

    private List<String> readTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) return List.of();
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {}).stream()
                    .filter(StringUtils::hasText).map(String::trim).distinct().toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeTags(List<String> tags) {
        try {
            List<String> normalized = tags == null ? List.of() : tags.stream()
                    .filter(StringUtils::hasText).map(String::trim).distinct().toList();
            if (normalized.isEmpty()) throw new BaseException(HttpStatus.BAD_REQUEST, "请至少选择一个课程标签");
            return objectMapper.writeValueAsString(normalized);
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程标签保存失败");
        }
    }

    private void validateAvailableTags(List<String> tags) {
        List<String> requested = tags == null ? List.of() : tags.stream()
                .filter(StringUtils::hasText).map(String::trim).distinct().toList();
        if (requested.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "请至少选择一个课程标签");
        }
        List<String> available = listAvailableTags();
        if (!available.containsAll(requested)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程标签必须来自已有课程标签");
        }
    }
}
