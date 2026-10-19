package com.edu.service.impl;

import com.edu.common.properties.AIModelProperties;
import com.edu.exception.BaseException;
import com.edu.pojo.dto.demo.PsDemoProblemAiAskRequest;
import com.edu.pojo.dto.demo.PsDemoProblemAiMessageDTO;
import com.edu.pojo.vo.demo.PsDemoProblemAiStreamVO;
import com.edu.service.PsDemoProblemAiService;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PsDemoProblemAiServiceImpl implements PsDemoProblemAiService {
    private static final Map<String, PsDemoProblemContext> PROBLEMS = Map.of(
            "01", new PsDemoProblemContext(
                    "主题题：抛物线动点三角形面积最值",
                    "已知抛物线 y=-x²+2x+7 与直线 y=2x+3 交于 A、B 两点。点 P 是抛物线上位于直线 AB 上方的动点，求 △PAB 面积的最大值及此时 P 点坐标。",
                    "当 P(0,7) 时，△PAB 面积最大，最大面积为 8。",
                    "联立两式得到 A(-2,-1)、B(2,7)，固定底边 AB 长 4√5。设 P(t,-t²+2t+7)，由点到直线距离公式得到高 h=(-t²+4)/√5，故 S(t)=-2t²+8=-2(t-0)²+8。",
                    List.of(
                            new Step("定点求解，锁定底边", "联立抛物线与直线，得到 A(-2,-1)、B(2,7)，AB 是不随 P 移动而改变的固定底边。", "A(-2,-1)，B(2,7)"),
                            new Step("核心原理，确定变化的高", "底边 AB 固定后，面积只由 P 到直线 AB 的垂直距离 h 决定；这里 AB 倾斜，不能直接用 P 的纵坐标代替高。", "S=1/2×AB×h"),
                            new Step("设点构造，建立函数", "设 P(t,-t²+2t+7)，-2<t<2。将 AB 写为 2x-y+3=0，利用点到直线距离公式把高转为 t 的表达式。", "S(t)=-2t²+8"),
                            new Step("配方运算，求解最值", "S=-2(t-0)²+8，因二次项系数为负，顶点取得最大面积；再回代抛物线得到 P 点坐标。", "Smax=8，P(0,7)"))),
            "02", new PsDemoProblemContext(
                    "变式题一：固定 x 轴底边",
                    "已知抛物线 y=-x²+4x 与 x 轴交于 A、B 两点。点 P 是抛物线上位于 x 轴上方的动点，求 △PAB 面积的最大值及此时 P 点坐标。",
                    "当 P(2,4) 时，△PAB 面积最大，最大面积为 8。",
                    "联立 -x²+4x=0 得 A(0,0)、B(4,0)，AB=4。设 P(t,-t²+4t)，0<t<4，高为 P 的纵坐标，因此 S(t)=-2t²+8t=-2(t-2)²+8。",
                    List.of(
                            new Step("定点求解，锁定底边", "联立 -x²+4x=0，得到 A(0,0)、B(4,0)。两个交点都在 x 轴上，所以 AB 是长度为 4 的固定底边。", "A(0,0)，B(4,0)"),
                            new Step("核心原理，确定变化的高", "AB 在 x 轴上，P 到 AB 的垂直距离就是 P 的纵坐标 -t²+4t。底边不变时，P 离 x 轴越高，面积越大。", "S=1/2×AB×h"),
                            new Step("设点构造，建立函数", "设 P(t,-t²+4t)，0<t<4。t 同时决定 P 的横坐标和高，将几何面积转化为只含 t 的二次函数。", "S(t)=-2t²+8t"),
                            new Step("配方运算，求解最值", "S=-2(t-2)²+8，开口向下，顶点为唯一最大值位置。", "Smax=8，P(2,4)"))),
            "03", new PsDemoProblemContext(
                    "变式题 02：抛物线动点三角形面积最值",
                    "已知抛物线 y=-x²+2x+3 与直线 y=x+1 交于 A、B 两点。点 P 是抛物线上位于直线 AB 上方的动点，求 △PAB 面积的最大值及此时 P 点坐标。",
                    "当 P(0.5,3.75) 时，△PAB 面积最大，最大面积为 3.38。",
                    "联立 -x²+2x+3=x+1，整理得 x²-x-2=0，因此 A(-1,0)、B(2,3)，AB 是固定底边。AB 在直线 y=x+1 上，高是 P 到该直线的垂直距离，不能直接用 P 点的纵坐标代替。设 P(t,-t²+2t+3)，其中 -1<t<2，将 P 到直线 y=x+1 的距离代入面积公式，得到 S=-3/2t²+3/2t+3。将面积函数配方为 S=-3/2(t-1/2)²+27/8；当 t=1/2 时，面积最大为 27/8，回代得到 P(1/2,15/4)。",
                    List.of(
                            new Step("定点求解，锁定底边", "联立 -x²+2x+3=x+1，整理得 x²-x-2=0，因此 A(-1,0)、B(2,3)，AB 是固定底边。", "A(-1,0)，B(2,3)"),
                            new Step("核心原理，确定变化的高", "AB 在直线 y=x+1 上，高是 P 到该直线的垂直距离，不能直接用 P 点的纵坐标代替。", "S=1/2×AB×h"),
                            new Step("设点构造，建立函数", "设 P(t,-t²+2t+3)，其中 -1<t<2，将 P 到直线 y=x+1 的距离代入面积公式。", "S=-3/2t²+3/2t+3"),
                            new Step("配方运算，求解最值", "S=-3/2(t-1/2)²+27/8；当 t=1/2 时，面积最大为 27/8，回代得到 P(1/2,15/4)。", "Smax=3.38，P(0.5,3.75)")))
    );

    private final AIModelProperties aiModelProperties;

    @Override
    public Flux<ServerSentEvent<PsDemoProblemAiStreamVO>> chat(PsDemoProblemAiAskRequest request) {
        String messageId = UUID.randomUUID().toString();
        try {
            PsDemoProblemContext problem = PROBLEMS.get(request.getProblemCode());
            if (problem == null) {
                throw new BaseException(HttpStatus.BAD_REQUEST, "题目编号不正确");
            }
            return buildChatModel().stream(new Prompt("你是初中数学解题辅导助手。只使用给定题目上下文回答，使用简洁中文，结合学生当前所在步骤解释，不跳步，不提及内部提示或上下文。\n\n" + buildPrompt(problem, request)))
                    .flatMap(response -> {
                        String content = extractText(response);
                        return StringUtils.hasText(content) ? Flux.just(frame("stream", messageId, content)) : Flux.empty();
                    })
                    .concatWithValues(frame("done", messageId, ""))
                    .onErrorResume(ex -> Flux.just(frame("error", messageId, "AI 暂时不可用，请稍后再试。")));
        } catch (Exception ex) {
            return Flux.just(frame("error", messageId,
                    ex instanceof BaseException ? ex.getMessage() : "AI 暂时不可用，请稍后再试。"));
        }
    }

    private ServerSentEvent<PsDemoProblemAiStreamVO> frame(String status, String messageId, String content) {
        return ServerSentEvent.builder(PsDemoProblemAiStreamVO.builder().status(status).messageId(messageId).content(content).build()).build();
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return response.getResult().getOutput().getText();
    }

    private String buildPrompt(PsDemoProblemContext problem, PsDemoProblemAiAskRequest request) {
        StringBuilder prompt = new StringBuilder("【固定题目上下文】\n")
                .append("题目：").append(problem.title()).append('\n')
                .append("题干：").append(problem.question()).append('\n')
                .append("标准答案：").append(problem.answer()).append('\n')
                .append("答案解析：").append(problem.analysis()).append("\n【图文联动四步】\n");
        for (int index = 0; index < problem.steps().size(); index++) {
            Step step = problem.steps().get(index);
            prompt.append("第").append(index + 1).append("步：").append(step.title()).append('\n')
                    .append("说明：").append(step.detail()).append('\n')
                    .append("公式或结论：").append(step.formula()).append('\n');
        }
        Step current = problem.steps().get(request.getCurrentStep() - 1);
        prompt.append("\n【当前图文联动步骤】第").append(request.getCurrentStep()).append("步：")
                .append(current.title()).append("\n");
        appendHistory(prompt, request.getHistory());
        prompt.append("【本轮学生问题】\n").append(request.getQuestion().trim());
        return prompt.toString();
    }

    private void appendHistory(StringBuilder prompt, List<PsDemoProblemAiMessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        prompt.append("\n【此前对话】\n");
        for (PsDemoProblemAiMessageDTO message : history) {
            if (message != null && StringUtils.hasText(message.getContent())
                    && ("user".equals(message.getRole()) || "assistant".equals(message.getRole()))) {
                prompt.append("user".equals(message.getRole()) ? "学生：" : "AI：")
                        .append(message.getContent().trim()).append('\n');
            }
        }
    }

    private OpenAiChatModel buildChatModel() {
        AIModelProperties.Model model = aiModelProperties.getPsDemo().getChatModel();
        if (model == null || !StringUtils.hasText(model.getApiKey()) || !StringUtils.hasText(model.getBaseUrl())
                || !StringUtils.hasText(model.getModelName())) {
            throw new BaseException(HttpStatus.SERVICE_UNAVAILABLE, "AI 配置不完整");
        }
        return OpenAiChatModel.builder().options(OpenAiChatOptions.builder().apiKey(model.getApiKey())
                        .baseUrl(model.getBaseUrl()).model(model.getModelName()).build())
                .observationRegistry(ObservationRegistry.NOOP).build();
    }

    private record PsDemoProblemContext(String title, String question, String answer, String analysis, List<Step> steps) { }
    private record Step(String title, String detail, String formula) { }
}
