package com.edu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.exception.BaseException;
import com.edu.mapper.EduGovPlanTaskMapper;
import com.edu.mapper.EduGovUserGoalMapper;
import com.edu.pojo.dto.UserInfoDTO;
import com.edu.pojo.dto.gov.GovGoalSaveRequest;
import com.edu.pojo.dto.gov.GovPlanTaskSaveRequest;
import com.edu.pojo.po.EduGovPlanTaskPO;
import com.edu.pojo.po.EduGovUserGoalPO;
import com.edu.pojo.vo.gov.GovGoalVO;
import com.edu.pojo.vo.gov.GovPlanTaskVO;
import com.edu.service.GovGoalPlanService;
import com.edu.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GovGoalPlanServiceImpl implements GovGoalPlanService {
    private final EduGovUserGoalMapper goalMapper;
    private final EduGovPlanTaskMapper taskMapper;

    @Override
    public GovGoalVO getGoal() {
        EduGovUserGoalPO goal = goalMapper.selectOne(new LambdaQueryWrapper<EduGovUserGoalPO>()
                .eq(EduGovUserGoalPO::getUserId, userId()).eq(EduGovUserGoalPO::getDeleted, 0));
        return goal == null ? null : toGoal(goal);
    }

    @Override
    @Transactional
    public GovGoalVO saveGoal(GovGoalSaveRequest request) {
        Long userId = userId();
        EduGovUserGoalPO goal = goalMapper.selectOne(new LambdaQueryWrapper<EduGovUserGoalPO>()
                .eq(EduGovUserGoalPO::getUserId, userId).eq(EduGovUserGoalPO::getDeleted, 0));
        LocalDateTime now = LocalDateTime.now();
        if (goal == null) {
            goal = new EduGovUserGoalPO();
            goal.setUserId(userId); goal.setCreateTime(now); goal.setDeleted(0);
        }
        goal.setExamType(trim(request.getExamType()));
        goal.setExamName(request.getExamName().trim());
        goal.setExamDate(request.getExamDate());
        goal.setNote(trim(request.getNote()));
        goal.setUpdateTime(now);
        if (goal.getId() == null) goalMapper.insert(goal); else goalMapper.updateById(goal);
        return toGoal(goal);
    }

    @Override
    public List<GovPlanTaskVO> listTasks(LocalDate taskDate) {
        LambdaQueryWrapper<EduGovPlanTaskPO> query = new LambdaQueryWrapper<EduGovPlanTaskPO>()
                .eq(EduGovPlanTaskPO::getUserId, userId()).eq(EduGovPlanTaskPO::getDeleted, 0)
                .orderByAsc(EduGovPlanTaskPO::getTaskDate).orderByAsc(EduGovPlanTaskPO::getId);
        if (taskDate != null) query.eq(EduGovPlanTaskPO::getTaskDate, taskDate);
        return taskMapper.selectList(query).stream().map(this::toTask).toList();
    }

    @Override
    @Transactional
    public GovPlanTaskVO createTask(GovPlanTaskSaveRequest request) {
        EduGovPlanTaskPO task = new EduGovPlanTaskPO();
        LocalDateTime now = LocalDateTime.now();
        task.setUserId(userId()); task.setTaskDate(request.getTaskDate()); task.setTitle(request.getTitle().trim());
        task.setTaskType(trim(request.getTaskType())); task.setStatus(0); task.setDeleted(0);
        task.setCreateTime(now); task.setUpdateTime(now); taskMapper.insert(task);
        return toTask(task);
    }

    @Override
    @Transactional
    public GovPlanTaskVO updateTask(Long taskId, GovPlanTaskSaveRequest request) {
        EduGovPlanTaskPO task = requireTask(taskId);
        task.setTaskDate(request.getTaskDate()); task.setTitle(request.getTitle().trim()); task.setTaskType(trim(request.getTaskType()));
        task.setUpdateTime(LocalDateTime.now()); taskMapper.updateById(task); return toTask(task);
    }

    @Override
    @Transactional
    public GovPlanTaskVO toggleTask(Long taskId, boolean completed) {
        EduGovPlanTaskPO task = requireTask(taskId);
        task.setStatus(completed ? 1 : 0); task.setCompletedAt(completed ? LocalDateTime.now() : null);
        task.setUpdateTime(LocalDateTime.now()); taskMapper.updateById(task); return toTask(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        EduGovPlanTaskPO task = requireTask(taskId);
        task.setDeleted(1); task.setUpdateTime(LocalDateTime.now()); taskMapper.updateById(task);
    }

    private EduGovPlanTaskPO requireTask(Long id) {
        EduGovPlanTaskPO task = taskMapper.selectOne(new LambdaQueryWrapper<EduGovPlanTaskPO>()
                .eq(EduGovPlanTaskPO::getId, id).eq(EduGovPlanTaskPO::getUserId, userId()).eq(EduGovPlanTaskPO::getDeleted, 0));
        if (task == null) throw new BaseException(HttpStatus.NOT_FOUND, "任务不存在");
        return task;
    }

    private GovGoalVO toGoal(EduGovUserGoalPO goal) { return GovGoalVO.builder().id(goal.getId()).examType(goal.getExamType()).examName(goal.getExamName()).examDate(goal.getExamDate()).note(goal.getNote()).build(); }
    private GovPlanTaskVO toTask(EduGovPlanTaskPO task) { return GovPlanTaskVO.builder().id(task.getId()).taskDate(task.getTaskDate()).title(task.getTitle()).taskType(task.getTaskType()).status(task.getStatus()).completedAt(task.getCompletedAt()).build(); }
    private Long userId() { UserInfoDTO user = SecurityUtil.getLoginUser(UserInfoDTO.class); if (user == null || user.getUserId() == null) throw new BaseException(HttpStatus.UNAUTHORIZED, "请先登录"); return user.getUserId(); }
    private String trim(String value) { return value == null ? null : value.trim(); }
}
