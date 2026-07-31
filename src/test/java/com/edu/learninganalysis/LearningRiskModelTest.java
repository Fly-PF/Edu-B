package com.edu.learninganalysis;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningRiskModelTest {
    private final LearningRiskModel model = new LearningRiskModel();

    @Test
    void ranksInactiveLearnerAboveSteadyLearner() {
        LearningRiskModel.LearningRiskResult inactive = model.assess(new LearningRiskModel.LearningRiskInput(
                20, 68, 11, 4, 4, 0, 10
        ));
        LearningRiskModel.LearningRiskResult steady = model.assess(new LearningRiskModel.LearningRiskInput(
                88, 68, 1, 14, 4, 3, 130
        ));

        assertEquals("HIGH", inactive.level());
        assertEquals("LOW", steady.level());
        assertTrue(inactive.score() > steady.score());
        assertEquals("学习间隔", inactive.factors().getFirst().label());
    }

    @Test
    void seededDemoPasswordUsesTheDeclaredValue() {
        String seedPassword = "$2a$10$fPLyrSvzxqgYAZi7t48j5u/BUvXScaTdytk1nbE80FhRyUQPcE4Hi";

        assertTrue(new BCryptPasswordEncoder().matches("123456", seedPassword));
    }
}
