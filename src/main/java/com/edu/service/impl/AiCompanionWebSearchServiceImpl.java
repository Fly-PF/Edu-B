package com.edu.service.impl;

import com.edu.common.properties.AIModelProperties;
import com.edu.pojo.vo.ai.AiCompanionWebSource;
import com.edu.service.AiCompanionWebSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses a public RSS result feed. A provider API key is deliberately not required for
 * the local demonstration, and failures simply leave the answer in course-only mode.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCompanionWebSearchServiceImpl implements AiCompanionWebSearchService {
    private static final int MAX_SOURCES = 3;
    private static final int MAX_QUESTION_LENGTH = 240;
    private final AIModelProperties aiModelProperties;

    @Override
    public List<AiCompanionWebSource> search(String question) {
        AIModelProperties.WebSearch properties = aiModelProperties.getCompanion().getWebSearch();
        if (properties == null || !properties.isEnabled() || !StringUtils.hasText(properties.getUrl()) || !isSafeQuery(question)) {
            return List.of();
        }
        try {
            String query = URLEncoder.encode(question.trim().substring(0, Math.min(question.trim().length(), MAX_QUESTION_LENGTH)), StandardCharsets.UTF_8);
            String separator = properties.getUrl().contains("?") ? "&" : "?";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getUrl() + separator + "format=rss&q=" + query))
                    .timeout(Duration.ofMillis(Math.max(1000, properties.getTimeout())))
                    .header("User-Agent", "EduF-CourseCompanion/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(1000, properties.getTimeout())))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }
            return parseRss(response.body());
        } catch (Exception ex) {
            log.info("联网资料检索暂时不可用：{}", ex.getMessage());
            return List.of();
        }
    }

    private List<AiCompanionWebSource> parseRss(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        NodeList items = document.getElementsByTagName("item");
        List<AiCompanionWebSource> sources = new ArrayList<>();
        for (int index = 0; index < items.getLength() && sources.size() < MAX_SOURCES; index++) {
            Element item = (Element) items.item(index);
            String title = text(item, "title");
            String url = text(item, "link");
            String snippet = stripHtml(text(item, "description"));
            if (StringUtils.hasText(title) && StringUtils.hasText(url) && StringUtils.hasText(snippet)) {
                sources.add(new AiCompanionWebSource(limit(title, 120), url, limit(snippet, 500)));
            }
        }
        return sources;
    }

    private String text(Element item, String tagName) {
        NodeList nodes = item.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String stripHtml(String value) {
        return value.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private String limit(String value, int length) {
        return value.length() <= length ? value : value.substring(0, length) + "…";
    }

    private boolean isSafeQuery(String value) {
        if (!StringUtils.hasText(value) || value.length() > MAX_QUESTION_LENGTH) {
            return false;
        }
        return !value.matches(".*(?:1\\d{10}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}).*");
    }
}
