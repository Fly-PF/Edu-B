package com.edu.service;

import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;

public interface TeacherAiService {
    LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request);

    GradingGenerateResponse generateGrading(GradingGenerateRequest request);
}
