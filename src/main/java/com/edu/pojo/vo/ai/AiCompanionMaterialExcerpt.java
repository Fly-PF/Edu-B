package com.edu.pojo.vo.ai;

/**
 * A small, traceable piece of course material selected for the current question.
 */
public record AiCompanionMaterialExcerpt(
        String resourceName,
        Integer pageNumber,
        String content
) {
}
