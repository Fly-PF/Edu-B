package com.edu.pojo.vo.ai;

/** A concrete next step generated from the student's course progress. */
public record AiCompanionStudySuggestion(
        String type,
        String title,
        String description,
        String prompt
) {
}
