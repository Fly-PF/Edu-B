package com.edu.util;

import com.edu.common.dto.RagTextChunkDTO;
import com.edu.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import com.edu.util.abstracts.AbstractTikaTextExtractUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class VttTextExtractUtil extends AbstractTikaTextExtractUtil {
    private static final Pattern TIMESTAMP_LINE = Pattern.compile(
            "^\\s*(?:\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}\\s+-->\\s+(?:\\d{2}:)?\\d{2}:\\d{2}\\.\\d{3}.*$");
    private static final Pattern CUE_TAG = Pattern.compile("<[^>]+>");
    private static final int SEGMENT_CHAR_LIMIT = 1000;

    public List<RagTextChunkDTO> extract(InputStream inputStream) {
        try {
            List<String> captions = readCaptionLines(inputStream);
            return buildChunks(captions);
        } catch (Exception ex) {
            log.error("Failed to extract VTT text", ex);
            if (ex instanceof BaseException baseException) {
                throw baseException;
            }
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "VTT text extraction failed");
        }
    }

    private List<String> readCaptionLines(InputStream inputStream) throws IOException {
        validateInputStream(inputStream, "VTT");
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(removeBom(line).trim());
            }
        }

        List<String> captions = new ArrayList<>();
        for (int index = 0; index < lines.size();) {
            String line = lines.get(index);
            if (line.isEmpty() || line.equalsIgnoreCase("WEBVTT") || line.startsWith("WEBVTT ")) {
                index++;
                continue;
            }
            if (line.equalsIgnoreCase("NOTE") || line.startsWith("NOTE ")
                    || line.equalsIgnoreCase("STYLE") || line.equalsIgnoreCase("REGION")) {
                index = skipBlock(lines, index + 1);
                continue;
            }
            if (!TIMESTAMP_LINE.matcher(line).matches() && index + 1 < lines.size()
                    && TIMESTAMP_LINE.matcher(lines.get(index + 1)).matches()) {
                index++;
                line = lines.get(index);
            }
            if (!TIMESTAMP_LINE.matcher(line).matches()) {
                index++;
                continue;
            }

            index++;
            StringBuilder caption = new StringBuilder();
            while (index < lines.size() && !lines.get(index).isEmpty()) {
                String captionLine = CUE_TAG.matcher(lines.get(index))
                        .replaceAll("")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .trim();
                if (StringUtils.hasText(captionLine)) {
                    if (caption.length() > 0) {
                        caption.append(System.lineSeparator());
                    }
                    caption.append(captionLine);
                }
                index++;
            }
            if (caption.length() > 0) {
                captions.add(caption.toString());
            }
        }
        return captions;
    }

    private int skipBlock(List<String> lines, int index) {
        while (index < lines.size() && !lines.get(index).isEmpty()) {
            index++;
        }
        return index;
    }

    private List<RagTextChunkDTO> buildChunks(List<String> captions) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String caption : captions) {
            if (current.length() > 0 && current.length() + caption.length() + 1 > SEGMENT_CHAR_LIMIT) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(System.lineSeparator());
            }
            current.append(caption);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        List<RagTextChunkDTO> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            result.add(new RagTextChunkDTO("字幕 " + (i + 1) + "/" + chunks.size(), chunks.get(i)));
        }
        return result;
    }

    private String removeBom(String line) {
        return line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF'
                ? line.substring(1) : line;
    }
}
