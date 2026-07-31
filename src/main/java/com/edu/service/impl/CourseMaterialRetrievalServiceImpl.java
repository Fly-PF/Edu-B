package com.edu.service.impl;

import com.edu.pojo.vo.ai.AiCompanionMaterialExcerpt;
import com.edu.pojo.vo.course.ResourceVO;
import com.edu.service.CourseMaterialRetrievalService;
import com.edu.service.CourseResourceStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A small local retriever for the course PDFs. It keeps the implementation deployable
 * without an external vector database while retaining a page number for every citation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMaterialRetrievalServiceImpl implements CourseMaterialRetrievalService {
    private static final int PDF_RESOURCE_TYPE = 2;
    private static final long MAX_PDF_BYTES = 16L * 1024 * 1024;
    private static final int MAX_EXCERPTS = 3;
    private static final int MAX_EXCERPT_LENGTH = 900;
    private static final List<String> COURSE_KEYWORDS = List.of(
            "机器学习", "实验", "样本", "标签", "特征", "数据集", "分类", "训练", "测试", "评估",
            "准确率", "偏差", "隐私", "模型", "预测", "背景", "错误", "改进", "算法", "数据", "过拟合", "泛化", "欠拟合"
    );

    private final CourseResourceStorageService storageService;
    private final Map<String, List<PageText>> pageCache = new ConcurrentHashMap<>();

    @Override
    public List<AiCompanionMaterialExcerpt> retrieve(List<ResourceVO> resources, String question) {
        if (resources == null || resources.isEmpty() || !StringUtils.hasText(question)) {
            return List.of();
        }
        List<String> keywords = keywords(question);
        List<ScoredExcerpt> candidates = new ArrayList<>();
        for (ResourceVO resource : resources) {
            if (!isPdf(resource) || !StringUtils.hasText(resource.getStoredUrl())) {
                continue;
            }
            for (PageText page : pageCache.computeIfAbsent(resource.getStoredUrl(), key -> extractPages(resource))) {
                for (String excerpt : splitIntoExcerpts(page.text())) {
                    int score = score(excerpt, keywords);
                    if (score > 0) {
                        candidates.add(new ScoredExcerpt(resource.getName(), page.pageNumber(), excerpt, score));
                    }
                }
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparingInt(ScoredExcerpt::score).reversed())
                .limit(MAX_EXCERPTS)
                .map(item -> new AiCompanionMaterialExcerpt(item.resourceName(), item.pageNumber(), item.content()))
                .toList();
    }

    private List<PageText> extractPages(ResourceVO resource) {
        byte[] pdf = storageService.readLocalBytes(resource.getStoredUrl(), MAX_PDF_BYTES);
        if (pdf == null || pdf.length == 0) {
            return List.of();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = normalize(stripper.getText(document));
                if (StringUtils.hasText(text)) {
                    pages.add(new PageText(page, text));
                }
            }
            return pages;
        } catch (IOException ex) {
            log.warn("无法解析课程 PDF，resource={}", resource.getStoredUrl(), ex);
            return List.of();
        }
    }

    private boolean isPdf(ResourceVO resource) {
        return Integer.valueOf(PDF_RESOURCE_TYPE).equals(resource.getType())
                || resource.getStoredUrl().toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private List<String> keywords(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        COURSE_KEYWORDS.stream().filter(normalized::contains).forEach(result::add);
        for (String part : normalized.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")) {
            if (part.length() >= 2 && !part.chars().allMatch(Character::isIdeographic)) {
                result.add(part);
            }
        }
        return new ArrayList<>(result);
    }

    private int score(String content, List<String> keywords) {
        String normalized = content.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : keywords) {
            int start = 0;
            while ((start = normalized.indexOf(keyword.toLowerCase(Locale.ROOT), start)) >= 0) {
                score++;
                start += keyword.length();
            }
        }
        return score;
    }

    private List<String> splitIntoExcerpts(String pageText) {
        List<String> paragraphs = List.of(pageText.split("(?<=[。！？；])"));
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String value = paragraph.trim();
            if (value.isBlank()) continue;
            if (current.length() > 0 && current.length() + value.length() > MAX_EXCERPT_LENGTH) {
                result.add(current.toString());
                current.setLength(0);
            }
            current.append(value);
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private record PageText(int pageNumber, String text) {
    }

    private record ScoredExcerpt(String resourceName, int pageNumber, String content, int score) {
    }
}
