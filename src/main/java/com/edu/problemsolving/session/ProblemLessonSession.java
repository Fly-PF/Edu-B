package com.edu.problemsolving.session;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
public class ProblemLessonSession {
    private String clientSessionId;
    private String wsToken;
    private Long userId;
    private Long problemId;
    private Integer problemVersion;
    private Map<String, Object> initialScene;
    @Builder.Default
    private int currentLessonStep = -1;
    @Builder.Default
    private int completedLessonStep = -1;
    @Builder.Default
    private volatile boolean lessonGenerating = false;
    @Builder.Default
    private volatile boolean questionGenerating = false;
    @Builder.Default
    private long lastQuestionAt = 0L;
    @Builder.Default
    private String sceneRevision = "scene-r1";
    @Builder.Default
    private AtomicLong sequence = new AtomicLong();
    @Builder.Default
    private Map<String, String> actionRevisions = new ConcurrentHashMap<>();
    @Builder.Default
    private List<QuestionTurn> questionHistory = new CopyOnWriteArrayList<>();

    public record QuestionTurn(String question, String answer) {
    }
}
