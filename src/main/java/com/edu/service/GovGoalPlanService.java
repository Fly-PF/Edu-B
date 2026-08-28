package com.edu.service;

import com.edu.pojo.dto.gov.GovGoalSaveRequest;
import com.edu.pojo.dto.gov.GovPlanTaskSaveRequest;
import com.edu.pojo.vo.gov.GovGoalVO;
import com.edu.pojo.vo.gov.GovPlanTaskVO;

import java.time.LocalDate;
import java.util.List;

public interface GovGoalPlanService {
    GovGoalVO getGoal();
    GovGoalVO saveGoal(GovGoalSaveRequest request);
    List<GovPlanTaskVO> listTasks(LocalDate taskDate);
    GovPlanTaskVO createTask(GovPlanTaskSaveRequest request);
    GovPlanTaskVO updateTask(Long taskId, GovPlanTaskSaveRequest request);
    GovPlanTaskVO toggleTask(Long taskId, boolean completed);
    void deleteTask(Long taskId);
}
