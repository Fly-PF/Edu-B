package com.edu.pojo.vo.ai;

/** A public web result used only when the course material has no matching answer. */
public record AiCompanionWebSource(
        String title,
        String url,
        String snippet
) {
}
