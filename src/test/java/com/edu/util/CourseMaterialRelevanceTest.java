package com.edu.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseMaterialRelevanceTest {
    @Test
    void matchesCoreCourseTopic() {
        assertTrue(CourseMaterialRelevance.matches(
                "请根据当前课程第一章的中文学习包，解释什么是人工智能，并说明人工智能有哪些主要能力。",
                "人工智能可以从图像、声音和文字中获取信息，并进行学习和推理。"));
    }

    @Test
    void rejectsUnrelatedScienceQuestion() {
        assertFalse(CourseMaterialRelevance.matches(
                "请解释量子纠缠是什么？",
                "本节介绍机器学习、海洋清洁、训练数据和分类模型。"));
    }
}
