package com.edu.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.course.ChapterCreateRequest;
import com.edu.pojo.dto.course.ChapterReorderItem;
import com.edu.pojo.dto.course.ChapterUpdateRequest;
import com.edu.pojo.dto.course.CourseCreateRequest;
import com.edu.pojo.dto.course.CourseStudyRecordRequest;
import com.edu.pojo.dto.course.CourseUpdateRequest;
import com.edu.pojo.dto.course.ResourceCreateRequest;
import com.edu.pojo.dto.course.ResourceUpdateRequest;
import com.edu.pojo.po.EduChapterPO;
import com.edu.pojo.po.EduCoursePO;
import com.edu.pojo.po.EduResourcePO;
import com.edu.pojo.po.EduStudyRecordPO;
import com.edu.pojo.po.SysUserPO;
import com.edu.pojo.vo.course.ChapterVO;
import com.edu.pojo.vo.course.CourseStudyRecordVO;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.pojo.vo.course.ResourceVO;
import com.edu.repository.CourseModuleRepository;
import com.edu.repository.EduCourseClassRepository;
import com.edu.repository.EduClassStudentRepository;
import com.edu.repository.EduStudyRecordRepository;
import com.edu.repository.SysUserRepository;
import com.edu.service.CourseResourceStorageService;
import com.edu.service.CourseService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_ARCHIVED = 2;
    private static final int PUBLIC_COURSE = 1;
    private static final String ROLE_TEACHER = "TEACHER";

    private final CourseModuleRepository courseRepository;
    private final EduCourseClassRepository courseClassRepository;
    private final EduClassStudentRepository classStudentRepository;
    private final EduStudyRecordRepository studyRecordRepository;
    private final SysUserRepository userRepository;
    private final CourseResourceStorageService storageService;
    private final ObjectMapper objectMapper;

    @Override
    public List<CourseVO> listPublicCourses(String keyword, String grade, Integer difficulty, Integer courseType) {
        return listPublicCourses(keyword, grade, difficulty, courseType, null, false);
    }

    @Override
    public List<CourseVO> listPublicCourses(
            String keyword,
            String grade,
            Integer difficulty,
            Integer courseType,
            String tags,
            boolean matchAll
    ) {
        validateCourseFilter(difficulty, courseType);
        UserInfoDTO user = currentUserOrNull();
        List<String> selectedTags = parseTags(tags);
        return courseRepository.selectPublishedCourses(keyword, grade, difficulty, courseType).stream()
                .map(course -> toCourseVO(course, user))
                .filter(course -> matchesTags(course.getTags(), selectedTags, matchAll))
                .toList();
    }

    @Override
    public CourseVO getCourse(Long courseId) {
        UserInfoDTO user = currentUserOrNull();
        EduCoursePO course = requireCourse(courseId);
        if (!canViewCourse(course, user)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权查看该课程");
        }
        return toCourseVO(course, user);
    }

    @Override
    public List<ChapterVO> listCourseChapters(Long courseId) {
        UserInfoDTO user = currentUserOrNull();
        EduCoursePO course = requireCourse(courseId);
        if (!canViewCourse(course, user)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权查看该课程目录");
        }
        return buildChapterVOs(courseId);
    }

    @Override
    public List<ResourceVO> listChapterResources(Long chapterId) {
        UserInfoDTO user = currentUser();
        if (chapterId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "章节ID不能为空");
        }
        EduChapterPO chapter = courseRepository.selectChapterById(chapterId);
        if (chapter == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "章节不存在");
        }
        EduCoursePO course = requireCourse(chapter.getCourseId());
        if (!canViewCourse(course, user)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权查看该章节资源");
        }
        return courseRepository.selectResourcesByChapterId(chapterId).stream()
                .map(this::toResourceVO)
                .toList();
    }

    @Override
    public List<CourseStudyRecordVO> listStudyRecords(Long courseId) {
        UserInfoDTO user = currentUser();
        EduCoursePO course = requireCourse(courseId);
        if (!canViewCourse(course, user)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权查看该课程学习记录");
        }
        return studyRecordRepository.selectRecordsByStudentId(user.getUserId()).stream()
                .filter(record -> Objects.equals(record.getCourseId(), courseId))
                .map(this::toStudyRecordVO)
                .toList();
    }

    @Override
    @Transactional
    public CourseStudyRecordVO saveStudyRecord(Long courseId, CourseStudyRecordRequest request) {
        UserInfoDTO user = currentUser();
        EduCoursePO course = requireCourse(courseId);
        if (!canViewCourse(course, user)) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权学习该课程");
        }
        if (request == null || request.getChapterId() == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "章节ID不能为空");
        }

        EduChapterPO chapter = requireChapter(courseId, request.getChapterId());
        Long resourceId = request.getResourceId();
        if (resourceId != null) {
            requireResource(chapter.getId(), resourceId);
        }

        Integer progress = clampProgress(request.getProgress());
        Integer finishStatus = request.getFinishStatus() == null
                ? (progress >= 100 ? 1 : 0)
                : (request.getFinishStatus() > 0 ? 1 : 0);
        if (finishStatus == 1) {
            progress = 100;
        }

        EduStudyRecordPO record = studyRecordRepository.selectStudyRecord(user.getUserId(), chapter.getId());
        LocalDateTime now = LocalDateTime.now();
        if (record == null) {
            record = EduStudyRecordPO.builder()
                    .studentId(user.getUserId())
                    .courseId(courseId)
                    .chapterId(chapter.getId())
                    .resourceId(resourceId)
                    .progress(progress)
                    .studyDuration(nonNegative(request.getStudyDuration()))
                    .finishStatus(finishStatus)
                    .lastStudyTime(now)
                    .createTime(now)
                    .build();
            studyRecordRepository.insertStudyRecord(record);
        } else {
            record.setResourceId(resourceId == null ? record.getResourceId() : resourceId);
            record.setProgress(Math.max(defaultNumber(record.getProgress()), progress));
            record.setStudyDuration(Math.max(defaultNumber(record.getStudyDuration()), nonNegative(request.getStudyDuration())));
            record.setFinishStatus(Math.max(defaultNumber(record.getFinishStatus()), finishStatus));
            record.setLastStudyTime(now);
            studyRecordRepository.updateStudyRecord(record);
        }
        return toStudyRecordVO(record);
    }

    @Override
    @Transactional
    public CourseStudyRecordVO saveStudyRecord(CourseStudyRecordRequest request) {
        if (request == null || request.getCourseId() == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程ID不能为空");
        }
        return saveStudyRecord(request.getCourseId(), request);
    }

    @Override
    public List<CourseVO> listTeacherCourses(String status, String keyword) {
        UserInfoDTO user = requireTeacher();
        Integer statusValue = parseStatus(status);
        return courseRepository.selectTeacherCourses(user.getUserId(), statusValue, keyword).stream()
                .map(course -> toCourseVO(course, user))
                .toList();
    }

    @Override
    @Transactional
    public CourseVO createCourse(CourseCreateRequest request) {
        UserInfoDTO user = requireTeacher();
        LocalDateTime now = LocalDateTime.now();
        EduCoursePO course = EduCoursePO.builder()
                .courseName(request.getTitle().trim())
                .cover(request.getCoverUrl())
                .grade(request.getGrade().trim())
                .difficulty(request.getDifficulty())
                .courseType(request.getCourseType())
                .teacherId(user.getUserId())
                .intro(request.getDescription())
                .totalDuration(0)
                .totalChapter(0)
                .publicFlag(Objects.equals(request.getIsPublic(), PUBLIC_COURSE) ? PUBLIC_COURSE : 0)
                .status(STATUS_DRAFT)
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .extJson(writeTags("{}", request.getTags()))
                .seriesName(normalizeSeriesName(request.getSeriesName()))
                .seriesOrder(nonNegative(request.getSeriesOrder()))
                .likeCount(0)
                .build();
        courseRepository.insertCourse(course);
        return toCourseVO(course, user);
    }

    @Override
    @Transactional
    public CourseVO updateCourse(Long courseId, CourseUpdateRequest request) {
        UserInfoDTO user = requireTeacher();
        EduCoursePO course = requireOwnedCourse(courseId, user);
        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "课程名称不能为空");
            }
            course.setCourseName(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            course.setIntro(request.getDescription());
        }
        if (request.getTags() != null) {
            course.setExtJson(writeTags(course.getExtJson(), request.getTags()));
        }
        if (request.getCoverUrl() != null) {
            course.setCover(request.getCoverUrl());
        }
        if (request.getSeriesName() != null) {
            course.setSeriesName(normalizeSeriesName(request.getSeriesName()));
        }
        if (request.getSeriesOrder() != null) {
            course.setSeriesOrder(nonNegative(request.getSeriesOrder()));
        }
        if (request.getGrade() != null) {
            if (!StringUtils.hasText(request.getGrade())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "适配学段不能为空");
            }
            course.setGrade(request.getGrade().trim());
        }
        if (request.getDifficulty() != null) {
            course.setDifficulty(request.getDifficulty());
        }
        if (request.getCourseType() != null) {
            course.setCourseType(request.getCourseType());
        }
        if (request.getIsPublic() != null) {
            course.setPublicFlag(request.getIsPublic());
        }
        course.setUpdateBy(user.getUserId());
        course.setUpdateTime(LocalDateTime.now());
        courseRepository.updateCourse(course);
        return toCourseVO(course, user);
    }

    @Override
    @Transactional
    public CourseVO uploadCourseCover(Long courseId, MultipartFile file) {
        UserInfoDTO user = requireTeacher();
        EduCoursePO course = requireOwnedCourse(courseId, user);
        CourseResourceStorageService.StoredCourseFile storedFile = storageService.upload(courseId, file);
        if (!Objects.equals(storedFile.resourceType(), 3)) {
            storageService.delete(storedFile.objectName());
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程封面仅支持图片格式");
        }
        String previousCover = course.getCover();
        course.setCover(storedFile.objectName());
        course.setUpdateBy(user.getUserId());
        course.setUpdateTime(LocalDateTime.now());
        courseRepository.updateCourse(course);
        storageService.delete(previousCover);
        return toCourseVO(course, user);
    }

    @Override
    @Transactional
    public void deleteCourse(Long courseId) {
        UserInfoDTO user = requireTeacher();
        EduCoursePO course = requireOwnedCourse(courseId, user);
        /*
            throw new BaseException(HttpStatus.BAD_REQUEST, "只能删除草稿课程");
        }

        */
        List<EduChapterPO> chapters = courseRepository.selectChaptersByCourseId(courseId);
        List<Long> chapterIds = chapters.stream().map(EduChapterPO::getId).toList();
        List<EduResourcePO> resources = courseRepository.selectResourcesByChapterIds(chapterIds);

        courseClassRepository.deleteByCourseId(courseId);
        courseRepository.deleteStudyRecordsByChapterIds(chapterIds);
        courseRepository.deleteResourcesByChapterIds(chapterIds);
        courseRepository.deleteChaptersByCourseId(courseId);
        courseRepository.deleteCourse(courseId);

        resources.forEach(resource -> storageService.delete(resource.getResourceUrl()));
        storageService.delete(course.getCover());
    }

    @Override
    @Transactional
    public void deleteDraftCourse(Long courseId) {
        deleteCourse(courseId);
    }

    @Override
    @Transactional
    public CourseVO publishCourse(Long courseId) {
        UserInfoDTO user = requireTeacher();
        EduCoursePO course = requireOwnedCourse(courseId, user);
        List<EduChapterPO> chapters = courseRepository.selectChaptersByCourseId(courseId);
        if (chapters.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "发布失败：课程至少需要一个章节");
        }
        List<EduResourcePO> resources = courseRepository.selectResourcesByChapterIds(
                chapters.stream().map(EduChapterPO::getId).toList()
        );
        Set<Long> chapterIdsWithResource = resources.stream()
                .map(EduResourcePO::getChapterId)
                .collect(Collectors.toSet());
        List<String> emptyChapters = chapters.stream()
                .filter(chapter -> !chapterIdsWithResource.contains(chapter.getId()))
                .map(EduChapterPO::getChapterName)
                .toList();
        if (!emptyChapters.isEmpty()) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "发布失败：以下章节没有资源：" + String.join("、", emptyChapters));
        }

        course.setStatus(STATUS_PUBLISHED);
        if (course.getPublishTime() == null) {
            course.setPublishTime(LocalDateTime.now());
        }
        course.setUpdateBy(user.getUserId());
        course.setUpdateTime(LocalDateTime.now());
        applyCourseTotals(course, chapters);
        courseRepository.updateCourse(course);
        return toCourseVO(course, user);
    }

    @Override
    @Transactional
    public ChapterVO createChapter(Long courseId, ChapterCreateRequest request) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        LocalDateTime now = LocalDateTime.now();
        EduChapterPO chapter = EduChapterPO.builder()
                .courseId(courseId)
                .chapterName(request.getTitle().trim())
                .sort(request.getSortOrder() == null ? nextChapterSort(courseId) : request.getSortOrder())
                .duration(nonNegative(request.getDuration()))
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .extJson("{}")
                .build();
        courseRepository.insertChapter(chapter);
        refreshCourseTotals(courseId, user.getUserId());
        return toChapterVO(chapter, List.of());
    }

    @Override
    @Transactional
    public ChapterVO updateChapter(Long courseId, Long chapterId, ChapterUpdateRequest request) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        EduChapterPO chapter = requireChapter(courseId, chapterId);
        if (request.getTitle() != null) {
            if (!StringUtils.hasText(request.getTitle())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "章节名称不能为空");
            }
            chapter.setChapterName(request.getTitle().trim());
        }
        if (request.getDuration() != null) {
            chapter.setDuration(nonNegative(request.getDuration()));
        }
        if (request.getSortOrder() != null) {
            chapter.setSort(nonNegative(request.getSortOrder()));
        }
        chapter.setUpdateBy(user.getUserId());
        chapter.setUpdateTime(LocalDateTime.now());
        courseRepository.updateChapter(chapter);
        refreshCourseTotals(courseId, user.getUserId());
        return toChapterVO(chapter, courseRepository.selectResourcesByChapterId(chapterId));
    }

    @Override
    @Transactional
    public void deleteChapter(Long courseId, Long chapterId) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        EduChapterPO chapter = requireChapter(courseId, chapterId);
        List<EduResourcePO> resources = courseRepository.selectResourcesByChapterId(chapter.getId());
        courseRepository.deleteStudyRecordsByChapterIds(List.of(chapterId));
        courseRepository.deleteResourcesByChapterIds(List.of(chapterId));
        courseRepository.deleteChapter(chapterId);
        resources.forEach(resource -> storageService.delete(resource.getResourceUrl()));
        refreshCourseTotals(courseId, user.getUserId());
    }

    @Override
    @Transactional
    public void reorderChapters(Long courseId, List<ChapterReorderItem> items) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, EduChapterPO> chapterMap = courseRepository.selectChaptersByCourseId(courseId).stream()
                .collect(Collectors.toMap(EduChapterPO::getId, chapter -> chapter));
        if (items.stream().anyMatch(item -> !chapterMap.containsKey(item.getId()))) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "排序列表包含其他课程的章节");
        }
        for (ChapterReorderItem item : items) {
            EduChapterPO chapter = chapterMap.get(item.getId());
            chapter.setSort(nonNegative(item.getSortOrder()));
            chapter.setUpdateBy(user.getUserId());
            chapter.setUpdateTime(LocalDateTime.now());
            courseRepository.updateChapter(chapter);
        }
    }

    @Override
    @Transactional
    public ResourceVO createResource(Long courseId, Long chapterId, ResourceCreateRequest request) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        requireChapter(courseId, chapterId);
        LocalDateTime now = LocalDateTime.now();
        EduResourcePO resource = EduResourcePO.builder()
                .chapterId(chapterId)
                .resourceName(request.getName().trim())
                .resourceType(request.getType())
                .resourceUrl(request.getUrl().trim())
                .fileSize(request.getFileSize())
                .duration(nonNegative(request.getDuration()))
                .sort(request.getSortOrder() == null ? nextResourceSort(chapterId) : nonNegative(request.getSortOrder()))
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .extJson("{}")
                .build();
        courseRepository.insertResource(resource);
        return toResourceVO(resource);
    }

    @Override
    @Transactional
    public ResourceVO uploadResource(Long courseId, Long chapterId, MultipartFile file, Integer duration) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        requireChapter(courseId, chapterId);
        CourseResourceStorageService.StoredCourseFile storedFile = storageService.upload(courseId, file);
        LocalDateTime now = LocalDateTime.now();
        EduResourcePO resource = EduResourcePO.builder()
                .chapterId(chapterId)
                .resourceName(storedFile.originalName())
                .resourceType(storedFile.resourceType())
                .resourceUrl(storedFile.objectName())
                .fileSize(storedFile.size())
                .duration(nonNegative(duration))
                .sort(nextResourceSort(chapterId))
                .createBy(user.getUserId())
                .updateBy(user.getUserId())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .extJson("{}")
                .build();
        try {
            courseRepository.insertResource(resource);
        } catch (RuntimeException ex) {
            storageService.delete(storedFile.objectName());
            throw ex;
        }
        return toResourceVO(resource);
    }

    @Override
    @Transactional
    public ResourceVO updateResource(
            Long courseId,
            Long chapterId,
            Long resourceId,
            ResourceUpdateRequest request
    ) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        requireChapter(courseId, chapterId);
        EduResourcePO resource = requireResource(chapterId, resourceId);
        if (request.getName() != null) {
            if (!StringUtils.hasText(request.getName())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "资源名称不能为空");
            }
            resource.setResourceName(request.getName().trim());
        }
        if (request.getType() != null) {
            resource.setResourceType(request.getType());
        }
        if (request.getUrl() != null) {
            if (!StringUtils.hasText(request.getUrl())) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "资源地址不能为空");
            }
            String previousUrl = resource.getResourceUrl();
            resource.setResourceUrl(request.getUrl().trim());
            if (!Objects.equals(previousUrl, resource.getResourceUrl())) {
                storageService.delete(previousUrl);
            }
        }
        if (request.getFileSize() != null) {
            resource.setFileSize(request.getFileSize());
        }
        if (request.getDuration() != null) {
            resource.setDuration(nonNegative(request.getDuration()));
        }
        if (request.getSortOrder() != null) {
            resource.setSort(nonNegative(request.getSortOrder()));
        }
        resource.setUpdateBy(user.getUserId());
        resource.setUpdateTime(LocalDateTime.now());
        courseRepository.updateResource(resource);
        return toResourceVO(resource);
    }

    @Override
    @Transactional
    public void deleteResource(Long courseId, Long chapterId, Long resourceId) {
        UserInfoDTO user = requireTeacher();
        requireOwnedCourse(courseId, user);
        requireChapter(courseId, chapterId);
        EduResourcePO resource = requireResource(chapterId, resourceId);
        courseRepository.deleteResource(resourceId);
        storageService.delete(resource.getResourceUrl());
    }

    private List<ChapterVO> buildChapterVOs(Long courseId) {
        List<EduChapterPO> chapters = courseRepository.selectChaptersByCourseId(courseId);
        if (chapters.isEmpty()) {
            return List.of();
        }
        Map<Long, List<EduResourcePO>> resourcesByChapter = courseRepository.selectResourcesByChapterIds(
                        chapters.stream().map(EduChapterPO::getId).toList()
                ).stream()
                .collect(Collectors.groupingBy(EduResourcePO::getChapterId, LinkedHashMap::new, Collectors.toList()));
        return chapters.stream()
                .map(chapter -> toChapterVO(chapter, resourcesByChapter.getOrDefault(chapter.getId(), List.of())))
                .toList();
    }

    private ChapterVO toChapterVO(EduChapterPO chapter, List<EduResourcePO> resources) {
        return ChapterVO.builder()
                .id(chapter.getId())
                .courseId(chapter.getCourseId())
                .title(chapter.getChapterName())
                .chapterName(chapter.getChapterName())
                .sortOrder(defaultNumber(chapter.getSort()))
                .sort(defaultNumber(chapter.getSort()))
                .duration(defaultNumber(chapter.getDuration()))
                .progress(0)
                .finishStatus(0)
                .createdTime(chapter.getCreateTime())
                .resources(resources.stream().map(this::toResourceVO).toList())
                .build();
    }

    private ResourceVO toResourceVO(EduResourcePO resource) {
        return ResourceVO.builder()
                .id(resource.getId())
                .chapterId(resource.getChapterId())
                .name(resource.getResourceName())
                .resourceName(resource.getResourceName())
                .type(resource.getResourceType())
                .resourceType(resource.getResourceType())
                .url(storageService.createReadUrl(resource.getResourceUrl()))
                .resourceUrl(storageService.createReadUrl(resource.getResourceUrl()))
                .storedUrl(resource.getResourceUrl())
                .fileSize(resource.getFileSize())
                .duration(defaultNumber(resource.getDuration()))
                .sortOrder(defaultNumber(resource.getSort()))
                .createdTime(resource.getCreateTime())
                .build();
    }

    private CourseStudyRecordVO toStudyRecordVO(EduStudyRecordPO record) {
        return CourseStudyRecordVO.builder()
                .id(record.getId())
                .studentId(record.getStudentId())
                .courseId(record.getCourseId())
                .chapterId(record.getChapterId())
                .resourceId(record.getResourceId())
                .progress(defaultNumber(record.getProgress()))
                .studyDuration(defaultNumber(record.getStudyDuration()))
                .finishStatus(defaultNumber(record.getFinishStatus()))
                .lastStudyTime(record.getLastStudyTime())
                .createdTime(record.getCreateTime())
                .build();
    }

    private CourseVO toCourseVO(EduCoursePO course, UserInfoDTO user) {
        List<EduChapterPO> chapters = courseRepository.selectChaptersByCourseId(course.getId());
        List<Long> chapterIds = chapters.stream().map(EduChapterPO::getId).toList();
        long resourceCount = courseRepository.selectResourcesByChapterIds(chapterIds).size();
        SysUserPO teacher = course.getTeacherId() == null ? null : userRepository.selectUserById(course.getTeacherId());

        return CourseVO.builder()
                .id(course.getId())
                .teacherId(course.getTeacherId())
                .teacherName(teacher == null ? "" : teacher.getRealName())
                .title(course.getCourseName())
                .courseName(course.getCourseName())
                .description(course.getIntro())
                .intro(course.getIntro())
                .tags(readTags(course.getExtJson()))
                .coverUrl(storageService.createReadUrl(course.getCover()))
                .cover(storageService.createReadUrl(course.getCover()))
                .grade(course.getGrade())
                .difficulty(course.getDifficulty())
                .courseType(course.getCourseType())
                .totalDuration(defaultNumber(course.getTotalDuration()))
                .totalChapter(chapters.size())
                .resourceCount(resourceCount)
                .seriesName(course.getSeriesName())
                .seriesOrder(defaultNumber(course.getSeriesOrder()))
                .likeCount(defaultNumber(course.getLikeCount()))
                .publishedTime(course.getPublishTime())
                .status(statusText(course.getStatus()))
                .publicCourse(Objects.equals(course.getPublicFlag(), PUBLIC_COURSE))
                .isPublic(course.getPublicFlag())
                .createdTime(course.getCreateTime())
                .updatedTime(course.getUpdateTime())
                .build();
    }

    private void refreshCourseTotals(Long courseId, Long userId) {
        EduCoursePO course = requireCourse(courseId);
        List<EduChapterPO> chapters = courseRepository.selectChaptersByCourseId(courseId);
        applyCourseTotals(course, chapters);
        course.setUpdateBy(userId);
        course.setUpdateTime(LocalDateTime.now());
        courseRepository.updateCourse(course);
    }

    private void applyCourseTotals(EduCoursePO course, List<EduChapterPO> chapters) {
        course.setTotalChapter(chapters.size());
        course.setTotalDuration(chapters.stream()
                .map(EduChapterPO::getDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
    }

    private EduCoursePO requireOwnedCourse(Long courseId, UserInfoDTO user) {
        EduCoursePO course = requireCourse(courseId);
        if (!Objects.equals(course.getTeacherId(), user.getUserId())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权操作该课程");
        }
        return course;
    }

    private EduCoursePO requireCourse(Long courseId) {
        if (courseId == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程ID不能为空");
        }
        EduCoursePO course = courseRepository.selectCourseById(courseId);
        if (course == null) {
            throw new BaseException(HttpStatus.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private EduChapterPO requireChapter(Long courseId, Long chapterId) {
        EduChapterPO chapter = courseRepository.selectChapterById(chapterId);
        if (chapter == null || !Objects.equals(chapter.getCourseId(), courseId)) {
            throw new BaseException(HttpStatus.NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    private EduResourcePO requireResource(Long chapterId, Long resourceId) {
        EduResourcePO resource = courseRepository.selectResourceById(resourceId);
        if (resource == null || !Objects.equals(resource.getChapterId(), chapterId)) {
            throw new BaseException(HttpStatus.NOT_FOUND, "课程资源不存在");
        }
        return resource;
    }

    private boolean canViewCourse(EduCoursePO course, UserInfoDTO user) {
        if (isPublishedPublic(course)) return true;
        if (user == null || user.getUserId() == null) return false;
        return Objects.equals(course.getTeacherId(), user.getUserId()) || hasClassAccess(course, user);
    }

    private boolean hasClassAccess(EduCoursePO course, UserInfoDTO user) {
        if (user == null || user.getUserId() == null) return false;
        List<Long> assignedClassIds = courseClassRepository.selectByCourseId(course.getId()).stream()
                .map(cp -> cp.getClassId()).toList();
        if (assignedClassIds.isEmpty()) return false;
        return classStudentRepository.selectClassesByStudentId(user.getUserId()).stream()
                .anyMatch(cs -> assignedClassIds.contains(cs.getClassId()));
    }

    private boolean isPublishedPublic(EduCoursePO course) {
        return Objects.equals(course.getStatus(), STATUS_PUBLISHED)
                && Objects.equals(course.getPublicFlag(), PUBLIC_COURSE);
    }

    private UserInfoDTO requireTeacher() {
        UserInfoDTO user = currentUser();
        if (!ROLE_TEACHER.equals(user.getRoleCode())) {
            throw new BaseException(HttpStatus.FORBIDDEN, "仅教师可以管理课程");
        }
        return user;
    }

    private UserInfoDTO currentUser() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private UserInfoDTO currentUserOrNull() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        return user != null && user.getUserId() != null ? user : null;
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "draft" -> STATUS_DRAFT;
            case "published" -> STATUS_PUBLISHED;
            case "archived" -> STATUS_ARCHIVED;
            default -> throw new BaseException(HttpStatus.BAD_REQUEST, "课程状态不正确");
        };
    }

    private String statusText(Integer status) {
        if (Objects.equals(status, STATUS_PUBLISHED)) {
            return "published";
        }
        if (Objects.equals(status, STATUS_ARCHIVED)) {
            return "archived";
        }
        return "draft";
    }

    private int nextChapterSort(Long courseId) {
        return courseRepository.selectChaptersByCourseId(courseId).stream()
                .map(EduChapterPO::getSort)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private int nextResourceSort(Long chapterId) {
        return courseRepository.selectResourcesByChapterId(chapterId).stream()
                .map(EduResourcePO::getSort)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(-1) + 1;
    }

    private List<String> readTags(String extJson) {
        if (!StringUtils.hasText(extJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(extJson);
            JsonNode tagsNode = root.isArray() ? root : root.path("tags");
            if (!tagsNode.isArray()) {
                return List.of();
            }
            List<String> tags = new ArrayList<>();
            tagsNode.forEach(item -> {
                if (item.isTextual() && StringUtils.hasText(item.asText())) {
                    tags.add(item.asText());
                }
            });
            return tags.stream().distinct().toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeTags(String extJson, List<String> tags) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            if (StringUtils.hasText(extJson)) {
                JsonNode parsed = objectMapper.readTree(extJson);
                if (parsed.isObject()) {
                    root = (ObjectNode) parsed;
                }
            }
            List<String> normalizedTags = tags == null ? List.of() : tags.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .limit(8)
                    .toList();
            root.set("tags", objectMapper.valueToTree(normalizedTags));
            return objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return "{\"tags\":[]}";
        }
    }

    private void validateCourseFilter(Integer difficulty, Integer courseType) {
        if (difficulty != null && (difficulty < 1 || difficulty > 3)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程难度不正确");
        }
        if (courseType != null && (courseType < 1 || courseType > 3)) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程类型不正确");
        }
    }

    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) return List.of();
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private boolean matchesTags(List<String> courseTags, List<String> selectedTags, boolean matchAll) {
        if (selectedTags.isEmpty()) return true;
        List<String> normalizedCourseTags = courseTags == null ? List.of() : courseTags;
        return matchAll
                ? selectedTags.stream().allMatch(normalizedCourseTags::contains)
                : selectedTags.stream().anyMatch(normalizedCourseTags::contains);
    }

    private String normalizeSeriesName(String seriesName) {
        return StringUtils.hasText(seriesName) ? seriesName.trim() : null;
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int clampProgress(Integer value) {
        return value == null ? 0 : Math.max(0, Math.min(100, value));
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }
}
