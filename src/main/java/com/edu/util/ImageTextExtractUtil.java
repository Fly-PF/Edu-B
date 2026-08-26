package com.edu.util;

import com.edu.common.properties.AIModelProperties;
import com.edu.exception.BaseException;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
@RequiredArgsConstructor
public class ImageTextExtractUtil {
    private static final Tika TIKA = new Tika();
    private static final String PREFIX = "这里有图片，下面是根据图片内容转换的Markdown格式内容，可能会与图片内容有所差异，请注意：\n";
    private static final String DEFAULT_FALLBACK_PREFIX = PREFIX;
    private static final String PROMPT = """
            你是一个图片内容转 Markdown 的高精度识别器。
            请严格根据图片内容输出 Markdown，只输出结果，不要解释，不要添加前言或结尾说明。

            目标：把图片中的所有可识别信息尽可能完整、准确地转成结构化 Markdown。

            规则：
            1. 内容准确性永远优先于布局还原。文字、数字、公式、表格内容必须尽量忠实识别；如果还原布局会影响内容准确性，就放弃布局，优先保证内容正确。
            2. 在不影响内容准确性的前提下，按图片中的真实阅读顺序整理内容，尽量保留原有层级、标题、段落、列表和注释。
            3. 如果是文档、课件、截图或书页，尽量逐字转写，不要自行润色改写。
            4. 表格必须尽量转换为 Markdown 表格；若存在合并单元格、跨列跨行或结构过于复杂，可改用分点列表保留层级关系，但不要丢失内容。
            5. 流程图、结构图、思维导图、树状图等内容，优先用 mermaid 表达；如果关系不够明确，就用缩进列表和箭头符号重建结构。
            6. 如果图片中包含图表、曲线图、照片、截图、示意图等子图片，不要输出 Markdown 图片标签，不要输出 ![...](...)，也不要生成占位图片链接；请在子图片所在位置直接用文字描述其标题、坐标轴、图例、趋势、关键数值和可识别信息。
            7. 数学公式尽量使用 LaTeX：行内公式用 $...$，独立公式用 $$...$$；分式、上下标、积分、矩阵、求和等都要规范呈现。若公式无法清晰辨认，用 [公式不清晰] 标记，不要编造。
            8. 代码片段请使用 Markdown 代码块原样输出。
            9. 对模糊、遮挡、缺失、无法确认的内容，使用 [无法辨认] 或 [推测：...] 标注，不要捏造。
            10. 如果图片包含多个区域，请用小标题分区输出，保证内容清晰可读。
            11. 最终输出必须是 Markdown 正文。
            """;

    private final AIModelProperties aiModelProperties;

    public String extract(InputStream inputStream) {
        return extract(inputStream, true);
    }

    public String extract(InputStream inputStream, boolean logResult, String prefix) {
        return extractInternal(inputStream, logResult, prefix);
    }

    public String extract(InputStream inputStream, boolean logResult) {
        return extractInternal(inputStream, logResult, DEFAULT_FALLBACK_PREFIX);
    }

    private String extractInternal(InputStream inputStream, boolean logResult, String prefix) {
        if (inputStream == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "图片不能为空");
        }

        byte[] imageBytes;
        try {
            imageBytes = inputStream.readAllBytes();
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "图片读取失败");
        }

        if (imageBytes.length == 0) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "图片不能为空");
        }

        String contentType = detectImageContentType(imageBytes);
        AIModelProperties.Model model = aiModelProperties.getRag().getMultiModel();
        if (model == null || !StringUtils.hasText(model.getApiKey()) || !StringUtils.hasText(model.getBaseUrl())
                || !StringUtils.hasText(model.getModelName())
                || model.getModelType() != AIModelProperties.ModelType.MultiModel) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 多模态配置不完整，请检查 edu.ai-model.rag.multi-model 相关配置");
        }

        String markdown = requestMarkdown(model, imageBytes, contentType);
        if (!StringUtils.hasText(markdown)) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "图片内容识别失败");
        }
        markdown = prefix + markdown;
        if (logResult) {
            log.info("Extracted IMG markdown: {}", markdown);
        }
        return markdown;
    }

    private String requestMarkdown(AIModelProperties.Model model, byte[] imageBytes, String contentType) {
        try {
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .options(OpenAiChatOptions.builder()
                            .apiKey(model.getApiKey())
                            .baseUrl(model.getBaseUrl())
                            .model(model.getModelName())
                            .temperature(0.0)
                            .maxTokens(4000)
                            .build())
                    .observationRegistry(ObservationRegistry.NOOP)
                    .build();

            Media media = new Media(MimeTypeUtils.parseMimeType(contentType), new ByteArrayResource(imageBytes));
            Prompt prompt = new Prompt(UserMessage.builder().text(PROMPT).media(List.of(media)).build());

            ChatResponse response = chatModel.call(prompt);
            return response == null || response.getResult() == null
                    ? null
                    : response.getResult().getOutput().getText();
        } catch (Exception ex) {
            log.error("Image OCR request failed for model {}", model.getModelName(), ex);
            throw new BaseException(HttpStatus.BAD_GATEWAY, "图片内容识别服务调用失败：" + ex.getMessage());
        }
    }

    private String detectImageContentType(byte[] imageBytes) {
        String contentType = TIKA.detect(imageBytes);
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BaseException(HttpStatus.BAD_REQUEST, "仅支持图片文件");
        }
        return contentType;
    }

}
