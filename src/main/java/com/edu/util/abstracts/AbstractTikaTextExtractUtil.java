package com.edu.util.abstracts;

import com.edu.exception.BaseException;
import com.edu.common.dto.RagTextChunkDTO;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTikaTextExtractUtil {
    private static final int SEGMENT_CHAR_LIMIT = 1000;

    protected void validateInputStream(InputStream inputStream, String fileTypeName) {
        if (inputStream == null) {
            throw new BaseException(HttpStatus.BAD_REQUEST, fileTypeName + " file stream cannot be null");
        }
    }

    protected RagTextChunkDTO logPage(Logger log, String fileTypeName, int pageNum, int totalPages, String text) {
        String markdownText = normalizeMarkdownText(text);
        log.info("Extracted {} page {}/{}, content: {}", fileTypeName, pageNum, totalPages, markdownText);
        return new RagTextChunkDTO("页 " + pageNum + "/" + totalPages, markdownText);
    }

    protected List<RagTextChunkDTO> logParagraphSegments(Logger log, String fileTypeName, List<String> paragraphs) {
        List<TextSegment> segments = buildTextSegments(paragraphs);
        List<RagTextChunkDTO> chunks = new ArrayList<>();
        int totalSegments = segments.size();
        for (int i = 0; i < totalSegments; i++) {
            TextSegment segment = segments.get(i);
            log.info("Extracted {} segment {}/{}, paragraphs {}-{}, content: {}",
                    fileTypeName, i + 1, totalSegments, segment.startParagraph(), segment.endParagraph(), segment.text());
            chunks.add(new RagTextChunkDTO("段落 " + segment.startParagraph() + "-" + segment.endParagraph()
                    + "/" + paragraphCount(paragraphs), segment.text()));
        }
        return chunks;
    }

    protected List<RagTextChunkDTO> extractParagraphChunks(Logger log, String fileTypeName, InputStream inputStream) {
        try {
            return logParagraphSegments(log, fileTypeName, readParagraphs(inputStream));
        } catch (Exception ex) {
            throwExtractException(log, fileTypeName, ex);
            return List.of();
        }
    }

    protected List<String> readParagraphs(InputStream inputStream) throws IOException {
        validateInputStream(inputStream, "TEXT");
        List<String> paragraphs = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = removeBom(line);
                if (!StringUtils.hasText(line)) {
                    addParagraph(paragraphs, paragraph);
                    continue;
                }
                if (!paragraph.isEmpty()) {
                    paragraph.append(System.lineSeparator());
                }
                paragraph.append(line);
            }
        }
        addParagraph(paragraphs, paragraph);
        return paragraphs;
    }

    protected String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    protected String normalizeMarkdownText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> normalizedLines = new ArrayList<>();
        boolean lastBlank = true;
        for (String line : lines) {
            String normalizedLine = trimRight(line).replace('\t', ' ');
            if (!StringUtils.hasText(normalizedLine)) {
                if (!lastBlank) {
                    normalizedLines.add("");
                }
                lastBlank = true;
                continue;
            }
            normalizedLines.add(normalizedLine.stripLeading());
            lastBlank = false;
        }

        while (!normalizedLines.isEmpty() && normalizedLines.get(normalizedLines.size() - 1).isEmpty()) {
            normalizedLines.remove(normalizedLines.size() - 1);
        }
        return convertSimpleTablesToMarkdown(normalizedLines);
    }

    protected int countNonWhitespaceChars(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    protected void throwExtractException(Logger log, String fileTypeName, Exception ex) {
        log.error("Failed to extract {} text", fileTypeName, ex);
        throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, fileTypeName + " text extraction failed");
    }

    private List<TextSegment> buildTextSegments(List<String> paragraphs) {
        List<TextSegment> segments = new ArrayList<>();
        if (paragraphs == null || paragraphs.isEmpty()) {
            return segments;
        }

        StringBuilder segmentText = new StringBuilder();
        int segmentChars = 0;
        int startParagraph = 1;
        int currentParagraph = 1;
        for (String paragraph : paragraphs) {
            String normalizedParagraph = normalizeText(paragraph);
            if (!StringUtils.hasText(normalizedParagraph)) {
                currentParagraph++;
                continue;
            }

            if (segmentText.length() > 0) {
                segmentText.append(System.lineSeparator());
            }
            segmentText.append(normalizedParagraph);
            segmentChars += countNonWhitespaceChars(normalizedParagraph);

            if (segmentChars >= SEGMENT_CHAR_LIMIT) {
                segments.add(new TextSegment(startParagraph, currentParagraph, segmentText.toString()));
                segmentText.setLength(0);
                segmentChars = 0;
                startParagraph = currentParagraph + 1;
            }
            currentParagraph++;
        }

        if (segmentText.length() > 0) {
            segments.add(new TextSegment(startParagraph, currentParagraph - 1, segmentText.toString()));
        }
        return segments;
    }

    private int paragraphCount(List<String> paragraphs) {
        return paragraphs == null ? 0 : paragraphs.size();
    }

    private void addParagraph(List<String> paragraphs, StringBuilder paragraph) {
        if (!paragraph.isEmpty()) {
            paragraphs.add(paragraph.toString());
            paragraph.setLength(0);
        }
    }

    private String convertSimpleTablesToMarkdown(List<String> lines) {
        List<String> result = new ArrayList<>();
        int index = 0;
        while (index < lines.size()) {
            List<List<String>> tableRows = collectTableRows(lines, index);
            if (tableRows.size() >= 2) {
                result.addAll(toMarkdownTable(tableRows));
                index += tableRows.size();
                continue;
            }
            result.add(lines.get(index));
            index++;
        }
        return String.join(System.lineSeparator(), result).trim();
    }

    private List<List<String>> collectTableRows(List<String> lines, int startIndex) {
        List<List<String>> rows = new ArrayList<>();
        int columnCount = 0;
        for (int i = startIndex; i < lines.size(); i++) {
            List<String> columns = splitTableColumns(lines.get(i));
            if (columns.size() < 2) {
                break;
            }
            if (columnCount == 0) {
                columnCount = columns.size();
            } else if (columns.size() != columnCount) {
                break;
            }
            rows.add(columns);
        }
        return rows;
    }

    private List<String> splitTableColumns(String line) {
        if (!StringUtils.hasText(line) || !line.matches(".*\\S\\s{2,}\\S.*")) {
            return List.of();
        }
        String[] parts = line.trim().split("\\s{2,}");
        List<String> columns = new ArrayList<>();
        for (String part : parts) {
            String value = normalizeText(part);
            if (StringUtils.hasText(value)) {
                columns.add(value);
            }
        }
        return columns;
    }

    private List<String> toMarkdownTable(List<List<String>> rows) {
        List<String> table = new ArrayList<>();
        table.add(toMarkdownTableRow(rows.get(0)));
        table.add(toMarkdownSeparator(rows.get(0).size()));
        for (int i = 1; i < rows.size(); i++) {
            table.add(toMarkdownTableRow(rows.get(i)));
        }
        return table;
    }

    private String toMarkdownTableRow(List<String> columns) {
        List<String> escapedColumns = new ArrayList<>();
        for (String column : columns) {
            escapedColumns.add(column.replace("|", "\\|"));
        }
        return "| " + String.join(" | ", escapedColumns) + " |";
    }

    private String toMarkdownSeparator(int columnCount) {
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            columns.add("---");
        }
        return "| " + String.join(" | ", columns) + " |";
    }

    private String trimRight(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private String removeBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private record TextSegment(int startParagraph, int endParagraph, String text) {
    }
}
