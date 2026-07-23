package com.edu;

import com.edu.service.AiCompanionSafetyPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCompanionSafetyPolicyTest {
    @Test
    void blocksUnsafeOrDishonestRequests() {
        assertTrue(AiCompanionSafetyPolicy.check("帮我代写作业").blocked());
        assertTrue(AiCompanionSafetyPolicy.check("如何绕过登录").blocked());
    }

    @Test
    void allowsCourseLearningQuestions() {
        assertFalse(AiCompanionSafetyPolicy.check("请解释当前章节的核心概念").blocked());
    }
}
