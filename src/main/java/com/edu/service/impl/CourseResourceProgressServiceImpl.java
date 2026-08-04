package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.BaseException;
import com.edu.mapper.BlockProjectMapper;
import com.edu.mapper.EduClassStudentMapper;
import com.edu.mapper.EduCourseClassMapper;
import com.edu.mapper.EduResourceBlockProjectMapper;
import com.edu.mapper.EduResourceStudyRecordMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.course.ResourceStudyRecordRequest;
import com.edu.pojo.po.BlockProjectPO;
import com.edu.pojo.po.EduClassStudentPO;
import com.edu.pojo.po.EduCourseClassPO;
import com.edu.pojo.po.EduCoursePO;
import com.edu.pojo.po.EduResourceBlockProjectPO;
import com.edu.pojo.po.EduResourcePO;
import com.edu.pojo.po.EduResourceStudyRecordPO;
import com.edu.pojo.vo.course.ResourceStudyRecordVO;
import com.edu.pojo.vo.course.ChapterResourceProgressVO;
import com.edu.pojo.po.EduChapterPO;
import com.edu.repository.CourseModuleRepository;
import com.edu.service.CourseResourceProgressService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseResourceProgressServiceImpl implements CourseResourceProgressService {
    private static final int BLOCK_PROJECT_RESOURCE = 5;
    private static final int PUBLIC_PROJECT = 1;

    private final CourseModuleRepository courseRepository;
    private final EduResourceStudyRecordMapper recordMapper;
    private final EduResourceBlockProjectMapper blockResourceMapper;
    private final BlockProjectMapper blockProjectMapper;
    private final EduCourseClassMapper courseClassMapper;
    private final EduClassStudentMapper classStudentMapper;

    @Override
    public List<ResourceStudyRecordVO> listRecords(Long courseId, Long assignmentId) {
        UserInfoDTO user = requireStudent();
        long scope = requireAssignment(courseId, assignmentId, user.getUserId());
        return recordMapper.selectList(new LambdaQueryWrapper<EduResourceStudyRecordPO>()
                        .eq(EduResourceStudyRecordPO::getStudentId, user.getUserId())
                        .eq(EduResourceStudyRecordPO::getCourseId, courseId)
                        .eq(EduResourceStudyRecordPO::getAssignmentId, scope))
                .stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    public ResourceStudyRecordVO save(ResourceStudyRecordRequest request) {
        UserInfoDTO user = requireStudent();
        EduResourcePO resource = requireResource(request);
        long scope = requireAssignment(request.getCourseId(), request.getAssignmentId(), user.getUserId());
        return upsert(user.getUserId(), scope, resource, request, false);
    }

    @Override
    @Transactional
    public ResourceStudyRecordVO openBlockProject(ResourceStudyRecordRequest request) {
        UserInfoDTO user = requireStudent();
        EduResourcePO resource = requireBlockResource(request);
        long scope = requireAssignment(request.getCourseId(), request.getAssignmentId(), user.getUserId());
        String kind = requireAvailableBlockProject(resource).getStageJson();
        boolean free = kind != null && kind.contains("\"kind\":\"free\"");
        ResourceStudyRecordRequest update = new ResourceStudyRecordRequest();
        update.setProgress(free ? 100 : 10);
        update.setFinishStatus(free ? 1 : 0);
        update.setStudyDuration(request.getStudyDuration());
        return upsert(user.getUserId(), scope, resource, update, free);
    }

    @Override
    @Transactional
    public ResourceStudyRecordVO completeBlockProject(ResourceStudyRecordRequest request) {
        UserInfoDTO user = requireStudent();
        EduResourcePO resource = requireBlockResource(request);
        long scope = requireAssignment(request.getCourseId(), request.getAssignmentId(), user.getUserId());
        String stageJson = requireAvailableBlockProject(resource).getStageJson();
        if (stageJson == null || !stageJson.contains("\"kind\":\"interactive\"")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "当前项目不是互动任务基座");
        }
        ResourceStudyRecordRequest update = new ResourceStudyRecordRequest();
        update.setProgress(100);
        update.setFinishStatus(1);
        update.setStudyDuration(request.getStudyDuration());
        return upsert(user.getUserId(), scope, resource, update, true);
    }

    @Override
    public List<ChapterResourceProgressVO> summarizeChapters(Long studentId, Long courseId, Long assignmentId) {
        long scope = assignmentId == null ? 0L : assignmentId;
        List<EduChapterPO> chapters = courseRepository.selectChaptersByCourseId(courseId);
        if (chapters.isEmpty()) return List.of();
        List<Long> chapterIds = chapters.stream().map(EduChapterPO::getId).toList();
        Map<Long, List<EduResourceStudyRecordPO>> recordsByChapter = recordMapper.selectList(
                        new LambdaQueryWrapper<EduResourceStudyRecordPO>()
                                .eq(EduResourceStudyRecordPO::getStudentId, studentId)
                                .eq(EduResourceStudyRecordPO::getCourseId, courseId)
                                .eq(EduResourceStudyRecordPO::getAssignmentId, scope))
                .stream().collect(Collectors.groupingBy(EduResourceStudyRecordPO::getChapterId));
        Map<Long, List<EduResourcePO>> resourcesByChapter = courseRepository.selectResourcesByChapterIds(chapterIds)
                .stream().collect(Collectors.groupingBy(EduResourcePO::getChapterId));
        return chapters.stream().map(chapter -> {
            List<EduResourcePO> resources = resourcesByChapter.getOrDefault(chapter.getId(), List.of());
            List<EduResourceStudyRecordPO> records = recordsByChapter.getOrDefault(chapter.getId(), List.of());
            Map<Long, EduResourceStudyRecordPO> byResource = records.stream().collect(Collectors.toMap(
                    EduResourceStudyRecordPO::getResourceId, record -> record, (left, right) -> right));
            boolean hasRecords = !byResource.isEmpty();
            int progress = resources.isEmpty() ? 0 : Math.round((float) resources.stream()
                    .map(resource -> byResource.get(resource.getId())).filter(java.util.Objects::nonNull)
                    .mapToInt(record -> clamp(record.getProgress())).sum() / resources.size());
            boolean complete = !resources.isEmpty() && resources.stream().allMatch(resource -> {
                EduResourceStudyRecordPO record = byResource.get(resource.getId());
                return record != null && number(record.getFinishStatus()) == 1;
            });
            return ChapterResourceProgressVO.builder().chapterId(chapter.getId()).progress(progress)
                    .finishStatus(complete ? 1 : 0).hasResourceRecords(hasRecords).build();
        }).toList();
    }

    private EduResourcePO requireResource(ResourceStudyRecordRequest request) {
        if (request == null || request.getCourseId() == null || request.getChapterId() == null || request.getResourceId() == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "课程、章节和资源不能为空");
        }
        EduResourcePO resource = courseRepository.selectResourceById(request.getResourceId());
        if (resource == null || !Objects.equals(resource.getChapterId(), request.getChapterId())) {
            throw new BaseException(HttpStatus.NOT_FOUND, "课程资源不存在");
        }
        if (courseRepository.selectChapterById(request.getChapterId()) == null
                || !Objects.equals(courseRepository.selectChapterById(request.getChapterId()).getCourseId(), request.getCourseId())) {
            throw new BaseException(HttpStatus.NOT_FOUND, "课程章节不存在");
        }
        return resource;
    }

    private EduResourcePO requireBlockResource(ResourceStudyRecordRequest request) {
        EduResourcePO resource = requireResource(request);
        if (!Objects.equals(resource.getResourceType(), BLOCK_PROJECT_RESOURCE)
                || blockResourceMapper.selectById(resource.getId()) == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "当前资源不是积木项目");
        }
        return resource;
    }

    private BlockProjectPO requireAvailableBlockProject(EduResourcePO resource) {
        EduResourceBlockProjectPO relation = blockResourceMapper.selectById(resource.getId());
        BlockProjectPO project = relation == null ? null : blockProjectMapper.selectById(relation.getProjectId());
        if (project == null || Objects.equals(project.getDeleted(), 1) || !Objects.equals(project.getVisibility(), PUBLIC_PROJECT)) {
            throw new BaseException(HttpStatus.GONE, "项目已不可用，请联系教师重新选择");
        }
        return project;
    }

    private long requireAssignment(Long courseId, Long assignmentId, Long studentId) {
        long scope = assignmentId == null ? 0L : assignmentId;
        if (scope == 0L) {
            EduCoursePO course = courseRepository.selectCourseById(courseId);
            if (course == null || !Objects.equals(course.getStatus(), 1) || !Objects.equals(course.getPublicFlag(), 1)) {
                throw new BaseException(HttpStatus.FORBIDDEN, "无权访问该课程资源");
            }
            return scope;
        }
        EduCourseClassPO assignment = courseClassMapper.selectById(scope);
        if (assignment == null || !Objects.equals(assignment.getCourseId(), courseId)
                || classStudentMapper.selectOne(new LambdaQueryWrapper<EduClassStudentPO>()
                .eq(EduClassStudentPO::getClassId, assignment.getClassId())
                .eq(EduClassStudentPO::getStudentId, studentId)) == null) {
            throw new BaseException(HttpStatus.FORBIDDEN, "无权访问该班级课程任务");
        }
        return scope;
    }

    private ResourceStudyRecordVO upsert(Long studentId, long assignmentId, EduResourcePO resource,
                                         ResourceStudyRecordRequest request, boolean completed) {
        EduResourceStudyRecordPO record = recordMapper.selectOne(new LambdaQueryWrapper<EduResourceStudyRecordPO>()
                .eq(EduResourceStudyRecordPO::getStudentId, studentId)
                .eq(EduResourceStudyRecordPO::getAssignmentId, assignmentId)
                .eq(EduResourceStudyRecordPO::getResourceId, resource.getId()));
        int requestedProgress = clamp(request.getProgress());
        int requestedFinish = completed || number(request.getFinishStatus()) > 0 ? 1 : 0;
        LocalDateTime now = LocalDateTime.now();
        if (record == null) {
            record = EduResourceStudyRecordPO.builder().studentId(studentId).assignmentId(assignmentId)
                    .courseId(courseRepository.selectChapterById(resource.getChapterId()).getCourseId())
                    .chapterId(resource.getChapterId()).resourceId(resource.getId())
                    .progress(requestedFinish == 1 ? 100 : requestedProgress)
                    .studyDuration(Math.max(0, number(request.getStudyDuration())))
                    .finishStatus(requestedFinish).lastStudyTime(now).createTime(now).build();
            recordMapper.insert(record);
        } else {
            record.setProgress(Math.max(number(record.getProgress()), requestedFinish == 1 ? 100 : requestedProgress));
            record.setStudyDuration(Math.max(number(record.getStudyDuration()), Math.max(0, number(request.getStudyDuration()))));
            record.setFinishStatus(Math.max(number(record.getFinishStatus()), requestedFinish));
            record.setLastStudyTime(now);
            recordMapper.updateById(record);
        }
        return toView(record);
    }

    private ResourceStudyRecordVO toView(EduResourceStudyRecordPO record) {
        return ResourceStudyRecordVO.builder().resourceId(record.getResourceId()).assignmentId(record.getAssignmentId())
                .chapterId(record.getChapterId()).progress(number(record.getProgress()))
                .studyDuration(number(record.getStudyDuration())).finishStatus(number(record.getFinishStatus()))
                .lastStudyTime(record.getLastStudyTime()).build();
    }

    private UserInfoDTO requireStudent() {
        UserInfoDTO user = SecurityUtil.getLoginUser();
        if (user == null || user.getUserId() == null) throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录");
        return user;
    }

    private static int number(Integer value) { return value == null ? 0 : value; }
    private static int clamp(Integer value) { return Math.max(0, Math.min(100, number(value))); }
}
