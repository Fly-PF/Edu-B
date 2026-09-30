package com.edu.problemsolving.socket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.common.properties.AIModelProperties;
import com.edu.mapper.ProblemVersionMapper;
import com.edu.pojo.po.problem.ProblemVersionPO;
import com.edu.problemsolving.session.ProblemLessonSession;
import com.edu.problemsolving.session.ProblemLessonSessionRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemSocketService {
    private static final List<String> STEP_IDS = List.of("UNDERSTAND", "MODEL", "DERIVE", "VERIFY");
    private static final List<String> STEP_TITLES = List.of("理解题意", "建立模型", "推导计算", "检验总结");
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final long QUESTION_INTERVAL_MILLIS = 1000;
    private static final Semaphore QUESTION_PERMITS = new Semaphore(8);
    private final ProblemLessonSessionRegistry lessonSessionRegistry;
    private final ProblemVersionMapper versionMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final AIModelProperties aiModelProperties;

    public void receive(String clientSessionId, ProblemSocketEvent event) {
        ProblemLessonSession session = lessonSessionRegistry.require(clientSessionId);
        String type = event == null || event.getType() == null ? "" : event.getType();
        log.info("Problem-solving socket event: clientSessionId={}, type={}", clientSessionId, type);
        if ("READY".equals(type)) {
            sendSnapshot(session);
        } else if ("START_LESSON".equals(type)) {
            startLesson(session);
        } else if ("NEXT_STEP".equals(type)) {
            nextStep(session);
        } else if ("SELECT_STEP".equals(type)) {
            selectStep(session, event);
        } else if ("ACK".equals(type)) {
            acknowledge(session, event);
        } else if ("QUESTION".equals(type)) {
            answerQuestion(session, event);
        }
    }

    private void startLesson(ProblemLessonSession session) {
        synchronized (session) {
            if (session.getCurrentLessonStep() >= 0 || session.isLessonGenerating()) return;
            session.setCurrentLessonStep(0);
            session.setLessonGenerating(true);
        }
        send(session, "lesson.plan", null, session.getSceneRevision(), Map.of("steps", STEP_IDS, "stepCount", 4));
        CompletableFuture.runAsync(() -> {
            try {
                for (int index = 0; index < STEP_IDS.size(); index++) {
                    session.setCurrentLessonStep(index);
                    teachStep(session, index);
                }
            } finally {
                session.setLessonGenerating(false);
            }
        });
    }

    private void nextStep(ProblemLessonSession session) {
        if (session.isLessonGenerating()) return;
        if (session.getCurrentLessonStep() < 0) return;
        if (session.getCurrentLessonStep() < session.getCompletedLessonStep()) {
            session.setCurrentLessonStep(session.getCurrentLessonStep() + 1);
            return;
        }
        if (session.getCompletedLessonStep() >= STEP_IDS.size() - 1) return;
        session.setCurrentLessonStep(session.getCompletedLessonStep() + 1);
        teachStep(session, session.getCurrentLessonStep());
    }

    private void selectStep(ProblemLessonSession session, ProblemSocketEvent event) {
        int stepIndex = STEP_IDS.indexOf(event.getStepId());
        if (stepIndex < 0 || stepIndex > session.getCompletedLessonStep()) return;
        session.setCurrentLessonStep(stepIndex);
    }

    private void teachStep(ProblemLessonSession session, int index) {
        Map<String, Object> definition = definition(session);
        String eventId = UUID.randomUUID().toString();
        String fallback = fallback(definition, index);
        String text = fallback;
        List<String> drawTargets = drawTargetsForStep(definition, index);
        String drawActionKey = "draw-step-" + STEP_IDS.get(index);
        try {
            String response = CompletableFuture.supplyAsync(() -> ChatClient.builder(buildChatModel()).build().prompt()
                    .system("你是 AI 解题导师。当前只能讲解给定题目的第 " + (index + 1) + " 步。"
                            + "只能使用题目事实，不能改写公式、答案或条件。若提供了视觉工具，必须调用一次绘制当前步骤后再回答；未提供时只输出文字。"
                            + physicsDrawingInstruction(definition))
                    .user("题面：" + string(definition.get("stem")) + "\n步骤：" + STEP_TITLES.get(index)
                            + "\n题目事实：" + string(definition.get("facts")) + "\n请用简洁中文完成本步讲解。")
                    .toolCallbacks(lessonDrawingTools(session, definition, eventId, drawActionKey, drawTargets).toArray(ToolCallback[]::new)).call().content())
                    .get(12, TimeUnit.SECONDS);
            if (StringUtils.hasText(response)) text = response;
        } catch (Exception ex) {
            log.warn("Problem-solving lesson fallback: clientSessionId={}, step={}", session.getClientSessionId(), index, ex);
        }
        applyPhysicsFallback(session, definition, eventId, index);
        if (!drawTargets.isEmpty() && !session.getActionRevisions().containsKey(eventId + ':' + drawActionKey)) {
            revealTargets(session, eventId, drawActionKey, drawTargets);
        }
        send(session, "lesson.step", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                "stepIndex", index, "stepId", STEP_IDS.get(index), "title", STEP_TITLES.get(index), "text", text,
                "completed", index == STEP_IDS.size() - 1));
        session.setCompletedLessonStep(index);
    }

    private String fallback(Map<String, Object> definition, int index) {
        String stem = string(definition.get("stem"));
        String answer = string(map(definition.get("facts")).get("answer"));
        return switch (index) {
            case 0 -> "先整理题目条件和目标：" + stem;
            case 1 -> "把题目条件转化为变量、图形和可计算的关系。";
            case 2 -> "根据已知关系完成推导与计算。";
            default -> "回代检查条件、单位和定义域。结论：" + answer;
        };
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        lessonSessionRegistry.removeTransport(event.getSessionId());
    }

    private void sendSnapshot(ProblemLessonSession session) {
        send(session, "scene.snapshot", null, session.getSceneRevision(), Map.of("scene", session.getInitialScene()));
    }

    private void acknowledge(ProblemLessonSession session, ProblemSocketEvent event) {
        if (event.getSequence() == null || event.getSequence() > session.getSequence().get()) {
            log.warn("Ignored invalid problem-solving ack: clientSessionId={}, sequence={}", session.getClientSessionId(), event.getSequence());
        }
    }

    private void answerQuestion(ProblemLessonSession session, ProblemSocketEvent event) {
        if (!StringUtils.hasText(event.getQuestion())) return;
        String eventId = StringUtils.hasText(event.getEventId()) ? event.getEventId() : UUID.randomUUID().toString();
        String question = event.getQuestion().trim();
        int requestedStep = STEP_IDS.indexOf(event.getCardId());
        int stepIndex = requestedStep >= 0 && requestedStep <= session.getCompletedLessonStep()
                ? requestedStep : Math.max(0, Math.min(session.getCurrentLessonStep(), STEP_IDS.size() - 1));
        String cardId = STEP_IDS.get(stepIndex);
        if (question.length() > MAX_QUESTION_LENGTH) {
            rejectQuestion(session, cardId, eventId, question, "问题不能超过 " + MAX_QUESTION_LENGTH + " 个字符。");
            return;
        }
        synchronized (session) {
            long now = System.currentTimeMillis();
            if (session.isQuestionGenerating()) {
                rejectQuestion(session, cardId, eventId, question, "上一轮追问仍在生成，请稍后再试。");
                return;
            }
            if (now - session.getLastQuestionAt() < QUESTION_INTERVAL_MILLIS) {
                rejectQuestion(session, cardId, eventId, question, "提问过于频繁，请稍后再试。");
                return;
            }
            if (!QUESTION_PERMITS.tryAcquire()) {
                rejectQuestion(session, cardId, eventId, question, "当前追问较多，请稍后再试。");
                return;
            }
            session.setQuestionGenerating(true);
            session.setLastQuestionAt(now);
        }
        ProblemVersionPO version = version(session);
        Map<String, Object> definition = definition(version);
        Map<String, Object> card = cards(definition.get("cards")).stream()
                .filter(item -> string(item.get("id")).equals(cardId)).findFirst().orElse(Map.of());
        String level = Set.of("basic", "sprint", "perfect").contains(event.getLevel()) ? event.getLevel() : "basic";
        String configuredFallback = string(map(card.get("explanations")).get(level));
        String fallback = StringUtils.hasText(configuredFallback) ? configuredFallback : fallback(definition, stepIndex);
        send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                "cardId", cardId, "question", question, "text", "", "turnId", eventId, "status", "start"));
        CompletableFuture.runAsync(() -> {
            StringBuilder answer = new StringBuilder();
            try {
                log.info("Problem-solving tutor started: clientSessionId={}, problemId={}, cardId={}", session.getClientSessionId(), session.getProblemId(), cardId);
                ChatClient.builder(buildChatModel()).build().prompt()
                        .system("你是 AI 解题导师。只能使用给定题面、标准答案、标准解析、当前步骤和审核讲解，不能修改公式、答案或条件。"
                                + "需要图示时只能调用已提供的视觉工具，不能描述任何未调用的图形变化。"
                                + physicsDrawingInstruction(definition)
                                + "必须使用规范 Markdown：先用“## 结论”概括，再用“## 推导”列出有序步骤，每一步独立成行。"
                                + "行内公式必须写成 $...$，独立公式必须单独一行写成 $$...$$；不得输出裸露的 LaTeX 命令（如 \\triangle、\\frac），不得重复公式、符号或文字。")
                        .user(questionContext(definition, version, stepIndex, card, fallback, session, question))
                        .toolCallbacks(visualTools(session, definition, eventId).toArray(ToolCallback[]::new))
                        .stream().content()
                        .filter(StringUtils::hasText)
                        .doOnNext(chunk -> {
                            answer.append(chunk);
                            send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                                    "cardId", cardId, "question", question, "text", chunk, "turnId", eventId, "status", "stream"));
                        })
                        .blockLast(Duration.ofSeconds(45));
                if (!StringUtils.hasText(answer)) {
                    answer.append(fallback);
                    send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                            "cardId", cardId, "question", question, "text", fallback, "turnId", eventId, "status", "stream"));
                }
                recordQuestionAnswer(session, question, answer.toString());
                send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                        "cardId", cardId, "question", question, "text", "", "turnId", eventId, "status", "done"));
                log.info("Problem-solving tutor completed: clientSessionId={}, problemId={}, cardId={}", session.getClientSessionId(), session.getProblemId(), cardId);
            } catch (Exception ex) {
                log.warn("Problem-solving tutor fallback: clientSessionId={}", session.getClientSessionId(), ex);
                if (!StringUtils.hasText(answer)) {
                    send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                            "cardId", cardId, "question", question, "text", fallback, "turnId", eventId, "status", "stream"));
                    answer.append(fallback);
                }
                recordQuestionAnswer(session, question, answer.toString());
                send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                        "cardId", cardId, "question", question, "text", "", "turnId", eventId, "status", "done"));
            } finally {
                session.setQuestionGenerating(false);
                QUESTION_PERMITS.release();
            }
        });
    }

    private void rejectQuestion(ProblemLessonSession session, String cardId, String eventId, String question, String message) {
        send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                "cardId", cardId, "question", question, "text", message, "turnId", eventId, "status", "stream"));
        send(session, "narration", session.getSceneRevision(), session.getSceneRevision(), Map.of(
                "cardId", cardId, "question", question, "text", "", "turnId", eventId, "status", "done"));
    }

    private String questionContext(Map<String, Object> definition, ProblemVersionPO version, int stepIndex,
                                   Map<String, Object> card, String fallback, ProblemLessonSession session, String question) {
        String title = StringUtils.hasText(string(card.get("title"))) ? string(card.get("title")) : STEP_TITLES.get(stepIndex);
        return "【题面】\n" + string(definition.get("stem"))
                + "\n【标准答案】\n" + string(version.getStandardAnswer())
                + "\n【标准解析】\n" + string(version.getAnswerAnalysis())
                + "\n【当前讲题步骤】\n第 " + (stepIndex + 1) + " 步：" + title
                + "\n【本步审核讲解】\n" + fallback
                + questionHistory(session) + "\n【学生问题】\n" + question;
    }

    private String questionHistory(ProblemLessonSession session) {
        List<ProblemLessonSession.QuestionTurn> history = session.getQuestionHistory();
        if (history.isEmpty()) return "";
        int start = Math.max(0, history.size() - 6);
        StringBuilder context = new StringBuilder("\n此前对话（仅用于理解当前追问，不得偏离题面）：");
        for (int index = start; index < history.size(); index++) {
            ProblemLessonSession.QuestionTurn turn = history.get(index);
            context.append("\n学生：").append(turn.question()).append("\n导师：").append(turn.answer());
        }
        return context.toString();
    }

    private void recordQuestionAnswer(ProblemLessonSession session, String question, String answer) {
        List<ProblemLessonSession.QuestionTurn> history = session.getQuestionHistory();
        history.add(new ProblemLessonSession.QuestionTurn(question, answer));
        if (history.size() > 6) history.remove(0);
    }

    private List<ToolCallback> visualTools(ProblemLessonSession session, Map<String, Object> definition, String eventId) {
        return visualTools(session, definition, eventId, Set.copyOf(strings(definition.get("visualCatalog"))));
    }

    private List<ToolCallback> stepDrawingTools(ProblemLessonSession session, String eventId, String actionKey, List<String> targets) {
        if (targets.isEmpty()) return List.of();
        return List.of(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder("drawSceneStep",
                        action -> revealTargets(session, eventId, actionKey, targets))
                .description("绘制当前讲题步骤已登记的图形对象。必须调用一次，不接受自定义对象或坐标。")
                .inputType(VisualActionRequest.class).build());
    }

    private List<ToolCallback> lessonDrawingTools(ProblemLessonSession session, Map<String, Object> definition,
                                                    String eventId, String actionKey, List<String> targets) {
        List<ToolCallback> physicsTools = physicsSceneTools(session, definition, eventId);
        if (!physicsTools.isEmpty()) return physicsTools;
        List<ToolCallback> callbacks = new ArrayList<>(stepDrawingTools(session, eventId, actionKey, targets));
        callbacks.addAll(dynamicSceneTools(session, definition, eventId));
        return callbacks;
    }

    private List<ToolCallback> visualTools(ProblemLessonSession session, Map<String, Object> definition, String eventId, Set<String> allowedAtStep) {
        List<ToolCallback> physicsTools = physicsSceneTools(session, definition, eventId);
        if (!physicsTools.isEmpty()) return physicsTools;
        Set<String> actions = new HashSet<>(strings(definition.get("visualCatalog")));
        actions.retainAll(allowedAtStep);
        List<ToolCallback> callbacks = new ArrayList<>();
        if (actions.contains("show-perpendicular-height")) {
            callbacks.add(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder("presentCoordinateScene",
                            action -> applyVisualAction(session, actions, eventId, action, Set.of("show-perpendicular-height")))
                    .description("展示当前坐标场景中已登记的垂线动作。仅可使用 actionKey=show-perpendicular-height。")
                    .inputType(VisualActionRequest.class).build());
        }
        if (actions.contains("highlight-AB")) {
            callbacks.add(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder("emphasizeSceneObjects",
                            action -> applyVisualAction(session, actions, eventId, action, Set.of("highlight-AB")))
                    .description("强调当前场景中已登记的对象。仅可使用 actionKey=highlight-AB。")
                    .inputType(VisualActionRequest.class).build());
        }
        Set<String> mechanicsActions = new HashSet<>();
        if (actions.contains("show-spring-force")) mechanicsActions.add("show-spring-force");
        if (!mechanicsActions.isEmpty()) {
            callbacks.add(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder("presentMechanicsScene",
                            action -> applyVisualAction(session, actions, eventId, action, mechanicsActions))
                    .description("展示当前弹簧测力计场景中已登记的受力图。仅可使用给定 actionKey。")
                    .inputType(VisualActionRequest.class).build());
        }
        callbacks.addAll(dynamicSceneTools(session, definition, eventId));
        return callbacks;
    }

    private List<ToolCallback> physicsSceneTools(ProblemLessonSession session, Map<String, Object> definition, String eventId) {
        String sceneType = string(map(definition.get("sceneDefinition")).get("sceneType"));
        Set<String> actions = physicsActions(sceneType);
        if (actions.isEmpty()) return List.of();
        List<ToolCallback> callbacks = new ArrayList<>();
        callbacks.add(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder(physicsToolName(sceneType),
                        action -> applyPhysicsAction(session, sceneType, eventId, action))
                .description("当前为固定物理模板“" + physicsSceneName(sceneType) + "”。仅可传入 actionKey，允许："
                        + String.join("、", actions) + "。不得传入 entities、坐标、图元或其他字段。")
                .inputType(VisualActionRequest.class).build());
        Map<String, Object> variables = map(map(definition.get("sceneDefinition")).get("variables"));
        Map<String, Object> templateVariables = variables.isEmpty() ? physicsTemplateVariables(sceneType) : variables;
        if (!templateVariables.isEmpty()) callbacks.add(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder("configurePhysicsTemplate",
                        action -> configureTemplateVariables(session, templateVariables, eventId, action))
                .description("根据题干配置当前物理模板的已登记参数。actionKey 必须为 set-template-variables；只能传 variables 中已登记的数值，不能传坐标、图元或其他参数。")
                .inputType(VisualActionRequest.class).build());
        return callbacks;
    }

    private Map<String, Object> physicsTemplateVariables(String sceneType) {
        return switch (sceneType) {
            case "mechanics2d" -> Map.of("force", Map.of("value", 3.2, "min", 0, "max", 100),
                    "blockPosition", Map.of("value", 0, "min", 0, "max", 1));
            case "physicsInclinedPlane" -> Map.of("inclinePosition", Map.of("value", .5, "min", 0, "max", 1));
            case "physicsCollision" -> Map.of("mass1", Map.of("value", 1, "min", .1, "max", 20),
                    "mass2", Map.of("value", 1, "min", .1, "max", 20),
                    "speed1", Map.of("value", 4, "min", -20, "max", 20),
                    "speed2", Map.of("value", 0, "min", -20, "max", 20),
                    "restitution", Map.of("value", 1, "min", 0, "max", 1));
            case "physicsPulley" -> Map.of("ropeEnd", Map.of("value", 0, "min", 0, "max", 1));
            case "physicsReflection" -> Map.of("incidentOffset", Map.of("value", .88, "min", -1, "max", 1));
            case "physicsMagneticField" -> Map.of("currentDirection", Map.of("value", 1, "min", -1, "max", 1));
            case "physicsCircuit" -> Map.of("voltage", Map.of("value", 12, "min", 1, "max", 48),
                    "r1", Map.of("value", 4, "min", 1, "max", 20), "r2", Map.of("value", 8, "min", 1, "max", 20));
            default -> Map.of();
        };
    }

    private VisualReceipt applyPhysicsAction(ProblemLessonSession session, String sceneType, String eventId, VisualActionRequest action) {
        Set<String> allowedActions = physicsActions(sceneType);
        if (action == null || !allowedActions.contains(action.actionKey()) || !isEmpty(action.entities()) || StringUtils.hasText(action.animation())) {
            return new VisualReceipt("rejected", session.getSceneRevision(), "当前物理模板只允许指定 actionKey");
        }
        String idempotencyKey = eventId + ':' + action.actionKey();
        String existing = session.getActionRevisions().get(idempotencyKey);
        if (existing != null) return new VisualReceipt("applied", existing, "图示已应用");
        String baseRevision = session.getSceneRevision();
        String nextRevision = baseRevision + "-q" + (session.getActionRevisions().size() + 1);
        send(session, "scene.command", baseRevision, nextRevision, Map.of("turnId", eventId,
                "operations", List.of(Map.of("op", "show", "target", action.actionKey()))));
        session.setSceneRevision(nextRevision);
        session.getActionRevisions().put(idempotencyKey, nextRevision);
        return new VisualReceipt("applied", nextRevision, "已应用 " + action.actionKey());
    }

    private void applyPhysicsFallback(ProblemLessonSession session, Map<String, Object> definition, String eventId, int stepIndex) {
        String sceneType = string(map(definition.get("sceneDefinition")).get("sceneType"));
        String action = physicsFallbackAction(sceneType, stepIndex);
        if (!StringUtils.hasText(action) || session.getActionRevisions().keySet().stream().anyMatch(key -> key.startsWith(eventId + ':'))) return;
        applyPhysicsAction(session, sceneType, eventId, new VisualActionRequest(action, null, null, null));
    }

    private VisualReceipt configureTemplateVariables(ProblemLessonSession session, Map<String, Object> definitions,
                                                       String eventId, VisualActionRequest action) {
        Map<String, Object> values = action == null ? Map.of() : map(action.variables());
        if (!"set-template-variables".equals(action == null ? "" : action.actionKey()) || values.isEmpty()
                || values.size() > definitions.size() || values.keySet().stream().anyMatch(name -> !definitions.containsKey(name))) {
            return new VisualReceipt("rejected", session.getSceneRevision(), "当前模板参数不合法");
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Map<String, Object> definition = map(definitions.get(entry.getKey()));
            if (!number(entry.getValue())) return new VisualReceipt("rejected", session.getSceneRevision(), "参数必须是数值");
            double value = Double.parseDouble(string(entry.getValue()));
            double min = number(definition.get("min")) ? Double.parseDouble(string(definition.get("min"))) : -1000;
            double max = number(definition.get("max")) ? Double.parseDouble(string(definition.get("max"))) : 1000;
            if (value < min || value > max) return new VisualReceipt("rejected", session.getSceneRevision(), "参数超出模板允许范围");
        }
        String idempotencyKey = eventId + ":set-template-variables";
        String existing = session.getActionRevisions().get(idempotencyKey);
        if (existing != null) return new VisualReceipt("applied", existing, "模板参数已应用");
        String baseRevision = session.getSceneRevision();
        String nextRevision = baseRevision + "-q" + (session.getActionRevisions().size() + 1);
        List<Map<String, Object>> operations = values.entrySet().stream().map(entry -> Map.<String, Object>of(
                "op", "setVariable", "name", entry.getKey(), "value", Double.parseDouble(string(entry.getValue())))).toList();
        send(session, "scene.command", baseRevision, nextRevision, Map.of("turnId", eventId, "operations", operations));
        session.setSceneRevision(nextRevision);
        session.getActionRevisions().put(idempotencyKey, nextRevision);
        return new VisualReceipt("applied", nextRevision, "已配置模板参数");
    }

    private String physicsFallbackAction(String sceneType, int stepIndex) {
        List<String> actions = switch (sceneType) {
            case "mechanics2d" -> List.of("show-spring-force", "show-spring-force", "show-spring-force", "show-spring-force");
            case "physicsInclinedPlane" -> List.of("show-gravity", "show-normal", "show-pulling-force", "show-friction");
            case "physicsCollision" -> List.of("show-before-state", "show-collision", "show-after-state");
            case "physicsPulley" -> List.of("show-weight", "show-tension", "show-force-balance", "show-force-balance");
            case "physicsReflection" -> List.of("show-normal", "show-incident-ray", "show-reflected-ray", "show-equal-angles");
            case "physicsMagneticField" -> List.of("show-current-direction", "show-right-hand-rule", "show-n-pole", "show-field-lines");
            case "physicsCircuit" -> List.of("show-voltage", "show-resistance", "show-current-direction", "show-ohm-law");
            default -> List.of();
        };
        return stepIndex >= 0 && stepIndex < actions.size() ? actions.get(stepIndex) : "";
    }

    private Set<String> physicsActions(String sceneType) {
        return switch (sceneType) {
            case "mechanics2d" -> Set.of("show-spring-force");
            case "physicsInclinedPlane" -> Set.of("show-gravity", "show-normal", "show-pulling-force", "show-friction");
            case "physicsCollision" -> Set.of("show-before-state", "show-collision", "show-after-state");
            case "physicsPulley" -> Set.of("show-tension", "show-weight", "show-force-balance");
            case "physicsReflection" -> Set.of("show-normal", "show-incident-ray", "show-reflected-ray", "show-equal-angles");
            case "physicsMagneticField" -> Set.of("show-current-direction", "show-right-hand-rule", "show-n-pole", "show-field-lines");
            case "physicsCircuit" -> Set.of("show-current-direction", "show-voltage", "show-resistance", "show-ohm-law");
            default -> Set.of();
        };
    }

    private String physicsToolName(String sceneType) {
        return switch (sceneType) {
            case "mechanics2d" -> "drawPhysicsHorizontalForce";
            case "physicsInclinedPlane" -> "drawPhysicsInclinedPlane";
            case "physicsCollision" -> "drawPhysicsCollision";
            case "physicsPulley" -> "drawPhysicsPulley";
            case "physicsReflection" -> "drawPhysicsReflection";
            case "physicsMagneticField" -> "drawPhysicsMagneticField";
            default -> "drawPhysicsCircuit";
        };
    }

    private String physicsSceneName(String sceneType) {
        return switch (sceneType) {
            case "mechanics2d" -> "水平面受力";
            case "physicsInclinedPlane" -> "斜面滑块";
            case "physicsCollision" -> "水平碰撞";
            case "physicsPulley" -> "动滑轮";
            case "physicsReflection" -> "平面镜反射";
            case "physicsMagneticField" -> "通电螺线管";
            case "physicsCircuit" -> "串联电路";
            default -> "物理";
        };
    }

    private String physicsDrawingInstruction(Map<String, Object> definition) {
        String sceneType = string(map(definition.get("sceneDefinition")).get("sceneType"));
        Set<String> actions = physicsActions(sceneType);
        if (actions.isEmpty()) return "";
        return "当前图示为固定物理模板“" + physicsSceneName(sceneType) + "”，装置本体和方向由系统固定。"
                + "只能调用当前模板的专属工具，且只可使用 actionKey：" + String.join("、", actions)
                + "；不得请求坐标、图元、其他模板动作或自行虚构图示。";
    }

    private boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private List<ToolCallback> dynamicSceneTools(ProblemLessonSession session, Map<String, Object> definition, String eventId) {
        String sceneType = string(map(definition.get("sceneDefinition")).get("sceneType"));
        if ("coordinate2d".equals(sceneType)) {
            Map<String, Object> variables = map(map(definition.get("sceneDefinition")).get("variables"));
            return variables.isEmpty() ? List.of() : List.of(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder("configureQuadraticTemplate",
                            action -> configureTemplateVariables(session, variables, eventId, action))
                    .description("根据题干配置二次函数动点模板的已登记参数。actionKey 必须为 set-template-variables；只能传 a、b、c、rootLeft、rootRight、t 等已登记数值，不能传坐标或图元。")
                    .inputType(VisualActionRequest.class).build());
        }
        if (!strings(definition.get("visualCatalog")).contains("draw-math-scene") || !isDynamicMathScene(sceneType)) return List.of();
        return List.of(FunctionToolCallback.<VisualActionRequest, VisualReceipt>builder("drawMathScene",
                        action -> applyDynamicScene(session, sceneType, eventId, action))
                .description(dynamicSceneDescription(sceneType)).inputType(VisualActionRequest.class).build());
    }

    private VisualReceipt applyDynamicScene(ProblemLessonSession session, String sceneType, String eventId, VisualActionRequest action) {
        if (action == null || !"draw-math-scene".equals(action.actionKey()) || !validEntities(sceneType, action.entities())) {
            return new VisualReceipt("rejected", session.getSceneRevision(), "图元格式或当前模板不合法");
        }
        String idempotencyKey = eventId + ":draw-math-scene";
        String existing = session.getActionRevisions().get(idempotencyKey);
        if (existing != null) return new VisualReceipt("applied", existing, "图示已应用");
        String baseRevision = session.getSceneRevision();
        String nextRevision = baseRevision + "-q" + (session.getActionRevisions().size() + 1);
        send(session, "scene.command", baseRevision, nextRevision, Map.of("turnId", eventId,
                "operations", List.of(Map.of("op", "setEntities", "entities", action.entities()))));
        session.setSceneRevision(nextRevision);
        session.getActionRevisions().put(idempotencyKey, nextRevision);
        return new VisualReceipt("applied", nextRevision, "已绘制数学图示");
    }

    private boolean isDynamicMathScene(String sceneType) {
        return Set.of("coordinatePlane2d", "coordinate3d", "geometry2d", "geometry3d", "statistics2d", "probabilityTree").contains(sceneType);
    }

    private String dynamicSceneDescription(String sceneType) {
        String types = switch (sceneType) {
            case "coordinatePlane2d", "geometry2d" -> "point、segment、line、polygon、circle、function";
            case "coordinate3d", "geometry3d" -> "point、segment、polygon";
            case "statistics2d" -> "bar（label 和 value）、linePoint（x、y、label）或 sector（label 和 value）";
            default -> "node（x、y、label）和 branch（from、to、label）";
        };
        return "根据题目在当前数学模板中绘图。必须 actionKey=draw-math-scene，entities 最多 24 个；仅允许：" + types + "。不得输出未列出的字段或图形。";
    }

    private boolean validEntities(String sceneType, List<Map<String, Object>> entities) {
        if (entities == null || entities.isEmpty() || entities.size() > 24) return false;
        Set<String> allowed = switch (sceneType) {
            case "coordinatePlane2d", "geometry2d" -> Set.of("point", "segment", "line", "polygon", "circle", "function");
            case "coordinate3d", "geometry3d" -> Set.of("point", "segment", "polygon");
            case "statistics2d" -> Set.of("bar", "linePoint", "sector");
            case "probabilityTree" -> Set.of("node", "branch");
            default -> Set.of();
        };
        return entities.stream().allMatch(item -> validEntity(item, allowed));
    }

    private boolean validEntity(Map<String, Object> entity, Set<String> allowed) {
        if (entity == null || !allowed.contains(string(entity.get("type"))) || !string(entity.get("id")).matches("[A-Za-z][A-Za-z0-9_-]{0,31}")) return false;
        if (string(entity.get("label")).length() > 32) return false;
        for (String key : List.of("x", "y", "z", "value", "radius")) {
            if (entity.containsKey(key) && (!number(entity.get(key)) || Math.abs(Double.parseDouble(string(entity.get(key)))) > 1000)) return false;
        }
        return true;
    }

    private boolean number(Object value) {
        try { Double.parseDouble(string(value)); return true; } catch (Exception ex) { return false; }
    }

    private void send(ProblemLessonSession session, String type, String baseRevision, String nextRevision,
                      Map<String, Object> payload) {
        synchronized (session) {
            ProblemSocketEnvelope envelope = ProblemSocketEnvelope.builder().eventId(UUID.randomUUID().toString())
                    .sequence(session.getSequence().incrementAndGet()).clientSessionId(session.getClientSessionId())
                    .type(type).baseRevision(baseRevision).nextRevision(nextRevision).payload(payload).build();
            messagingTemplate.convertAndSendToUser(session.getClientSessionId(), "/queue/problem-solving", envelope);
        }
    }

    private Map<String, Object> definition(ProblemLessonSession session) {
        return definition(version(session));
    }

    private ProblemVersionPO version(ProblemLessonSession session) {
        return versionMapper.selectOne(new LambdaQueryWrapper<ProblemVersionPO>()
                .eq(ProblemVersionPO::getProblemId, session.getProblemId())
                .eq(ProblemVersionPO::getVersion, session.getProblemVersion()).eq(ProblemVersionPO::getStatus, 1));
    }

    private Map<String, Object> definition(ProblemVersionPO version) {
        if (version == null) return Map.of();
        try {
            return objectMapper.readValue(version.getDefinitionJson(), new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private OpenAiChatModel buildChatModel() {
        AIModelProperties.Model model = aiModelProperties.getProblemSolving().getChatModel();
        if (model == null || !StringUtils.hasText(model.getApiKey()) || !StringUtils.hasText(model.getBaseUrl())
                || !StringUtils.hasText(model.getModelName())) throw new IllegalStateException("AI配置不完整");
        return OpenAiChatModel.builder().options(OpenAiChatOptions.builder().apiKey(model.getApiKey())
                        .baseUrl(model.getBaseUrl()).model(model.getModelName()).build())
                .observationRegistry(ObservationRegistry.NOOP).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) { return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of(); }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cards(Object value) { return value instanceof Collection<?> values ? values.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList() : List.of(); }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) { return value instanceof Collection<?> values ? values.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).toList() : List.of(); }
    private List<String> strings(Object value) { return value instanceof Collection<?> values ? values.stream().map(this::string).toList() : List.of(); }
    private String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private VisualReceipt applyVisualAction(ProblemLessonSession session, Set<String> catalog, String eventId,
                                            VisualActionRequest action, Set<String> allowedActions) {
        if (action == null || !allowedActions.contains(action.actionKey()) || !catalog.contains(action.actionKey())) {
            return new VisualReceipt("rejected", session.getSceneRevision(), "当前步骤不允许该视觉动作");
        }
        String idempotencyKey = eventId + ':' + action.actionKey();
        String existing = session.getActionRevisions().get(idempotencyKey);
        if (existing != null) return new VisualReceipt("applied", existing, "图示已应用");
        String baseRevision = session.getSceneRevision();
        String nextRevision = baseRevision + "-q" + (session.getActionRevisions().size() + 1);
        Map<String, Object> operation = switch (action.actionKey()) {
            case "show-perpendicular-height" -> Map.of("op", "show", "target", "altitude");
            case "highlight-AB" -> Map.of("op", "highlight", "target", "AB");
            case "show-spring-force" -> Map.of("op", "show", "target", "forceArrow");
            default -> throw new IllegalArgumentException("未知视觉动作");
        };
        send(session, "scene.command", baseRevision, nextRevision,
                Map.of("turnId", eventId, "operations", List.of(operation)));
        session.setSceneRevision(nextRevision);
        session.getActionRevisions().put(idempotencyKey, nextRevision);
        return new VisualReceipt("applied", nextRevision, "已应用 " + action.actionKey());
    }

    private VisualReceipt revealTargets(ProblemLessonSession session, String eventId, String actionKey, List<String> targets) {
        String idempotencyKey = eventId + ':' + actionKey;
        String existing = session.getActionRevisions().get(idempotencyKey);
        if (existing != null) return new VisualReceipt("applied", existing, "图示已应用");
        String baseRevision = session.getSceneRevision();
        String nextRevision = baseRevision + "-q" + (session.getActionRevisions().size() + 1);
        send(session, "scene.command", baseRevision, nextRevision,
                Map.of("turnId", eventId, "lessonStep", actionKey.startsWith("draw-step-"), "operations", List.of(Map.of("op", "reveal", "targets", targets))));
        session.setSceneRevision(nextRevision);
        session.getActionRevisions().put(idempotencyKey, nextRevision);
        return new VisualReceipt("applied", nextRevision, "已绘制当前步骤图示");
    }

    private List<String> drawTargetsForStep(Map<String, Object> definition, int index) {
        Map<String, Object> sceneDefinition = map(definition.get("sceneDefinition"));
        String sceneType = string(sceneDefinition.get("sceneType"));
        if (!physicsActions(sceneType).isEmpty()) return List.of();
        Map<String, Object> drawSteps = map(sceneDefinition.get("drawSteps"));
        String stepId = STEP_IDS.get(index);
        if (drawSteps.containsKey(stepId)) return strings(drawSteps.get(stepId));
        if ("coordinate2d".equals(sceneType)) {
            return switch (index) {
                case 0 -> List.of("parabola", "A", "B");
                case 1 -> List.of("P", "triangle", "baseLine");
                case 2 -> List.of("altitude", "areaChart");
                default -> List.of();
            };
        }
        if ("geometry2d".equals(sceneType)) {
            return maps(sceneDefinition.get("entities")).stream()
                    .filter(item -> index == 0 ? "point".equals(string(item.get("type"))) : index == 1)
                    .map(item -> string(item.get("id"))).filter(StringUtils::hasText).toList();
        }
        if ("chart2d".equals(sceneType) && index == 1) {
            return maps(sceneDefinition.get("series")).stream().map(item -> string(item.get("id")))
                    .filter(StringUtils::hasText).toList();
        }
        if ("mechanics2d".equals(sceneType) && index == 0) return List.of("mechanics");
        return List.of();
    }
}
