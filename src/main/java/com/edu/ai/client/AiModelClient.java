package com.edu.ai.client;

import com.edu.pojo.dto.teacherai.GradingGenerateRequest;
import com.edu.pojo.dto.teacherai.GradingGenerateResponse;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateRequest;
import com.edu.pojo.dto.teacherai.LessonPlanGenerateResponse;

/**
 * AI model abstraction. A real provider only needs to implement this interface
 * and register a mutually exclusive Spring bean.
 */
public interface AiModelClient {
    LessonPlanGenerateResponse generateLessonPlan(LessonPlanGenerateRequest request);

    GradingGenerateResponse generateGrading(GradingGenerateRequest request);
}
