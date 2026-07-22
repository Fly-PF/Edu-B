package com.edu.util;

import com.edu.common.dto.RagTextChunkDTO;
import com.edu.util.abstracts.AbstractTikaTextExtractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.GroupShape;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.TableCell;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.sl.usermodel.TextShape;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PptTextExtractUtil extends AbstractTikaTextExtractUtil {
    private static final int MIN_TEXT_CHAR_COUNT = 500;
    private static final String PPT_FALLBACK_PREFIX = "下面是根据原始内容转换的Markdown格式的内容，可能会与原始内容有所差异，请注意：\n";

    private final ImageTextExtractUtil imageTextExtractUtil;

    public List<RagTextChunkDTO> extract(InputStream inputStream) {
        validateInputStream(inputStream, "PPT");
        try (SlideShow<?, ?> slideShow = SlideShowFactory.create(inputStream)) {
            List<RagTextChunkDTO> chunks = new ArrayList<>();
            int totalPages = slideShow.getSlides().size();
            int pageNum = 1;
            for (Slide<?, ?> slide : slideShow.getSlides()) {
                String slideText = extractSlideText(slideShow, slide, pageNum, totalPages);
                chunks.add(logPage(log, "PPT", pageNum++, totalPages, slideText));
            }
            return chunks;
        } catch (Exception ex) {
            throwExtractException(log, "PPT", ex);
            return List.of();
        }
    }

    private String extractSlideText(SlideShow<?, ?> slideShow, Slide<?, ?> slide, int pageNum, int totalPages)
            throws IOException {
        String slideText = buildSlideText(slide);
        if (countNonWhitespaceChars(slideText) >= MIN_TEXT_CHAR_COUNT) {
            return "## 第 " + pageNum + " 页\n\n" + slideText;
        }
        return imageTextExtractUtil.extract(new ByteArrayInputStream(renderSlideToPng(slideShow, slide)), false,
                PPT_FALLBACK_PREFIX);
    }

    private String buildSlideText(Slide<?, ?> slide) {
        StringBuilder text = new StringBuilder();
        List<Shape<?, ?>> shapes = new ArrayList<>(slide.getShapes());
        shapes.sort(Comparator.comparingDouble((Shape<?, ?> shape) -> shape.getAnchor().getY())
                .thenComparingDouble((Shape<?, ?> shape) -> shape.getAnchor().getX()));
        for (Shape<?, ?> shape : shapes) {
            appendShapeText(shape, text);
        }
        return text.toString();
    }

    private void appendShapeText(Shape<?, ?> shape, StringBuilder text) {
        if (shape instanceof TableShape<?, ?> tableShape) {
            appendTableShapeText(tableShape, text);
            return;
        }
        if (shape instanceof TextShape<?, ?> textShape) {
            appendText(text, textShape.getText());
            return;
        }
        if (shape instanceof GroupShape<?, ?> groupShape) {
            for (Shape<?, ?> childShape : groupShape.getShapes()) {
                appendShapeText(childShape, text);
            }
        }
    }

    private void appendTableShapeText(TableShape<?, ?> tableShape, StringBuilder text) {
        List<List<String>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < tableShape.getNumberOfRows(); rowIndex++) {
            List<String> row = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < tableShape.getNumberOfColumns(); columnIndex++) {
                TableCell<?, ?> cell = tableShape.getCell(rowIndex, columnIndex);
                if (cell == null || cell.isMerged()) {
                    row.add("");
                    continue;
                }
                row.add(normalizeTableCellText(cell.getText()));
            }
            rows.add(row);
        }
        appendMarkdownTable(text, rows);
    }

    private void appendText(StringBuilder text, String value) {
        String normalizedValue = normalizeText(value);
        if (!StringUtils.hasText(normalizedValue) || normalizeText(text.toString()).contains(normalizedValue)) {
            return;
        }
        if (!text.isEmpty()) {
            text.append(System.lineSeparator());
        }
        text.append(normalizedValue);
    }

    private void appendMarkdownTable(StringBuilder text, List<List<String>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<String> header = rows.get(0);
        if (!text.isEmpty()) {
            text.append(System.lineSeparator()).append(System.lineSeparator());
        }
        text.append("|").append(String.join("|", escapeTableColumns(header))).append("|")
                .append(System.lineSeparator())
                .append("|")
                .append(String.join("|", repeatSeparator(header.size())))
                .append("|");
        for (int i = 1; i < rows.size(); i++) {
            text.append(System.lineSeparator())
                    .append("|")
                    .append(String.join("|", escapeTableColumns(rows.get(i))))
                    .append("|");
        }
    }

    private List<String> escapeTableColumns(List<String> columns) {
        List<String> escaped = new ArrayList<>();
        for (String column : columns) {
            escaped.add(normalizeText(column).replace("|", "\\|"));
        }
        return escaped;
    }

    private List<String> repeatSeparator(int size) {
        List<String> separator = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            separator.add("---");
        }
        return separator;
    }

    private String normalizeTableCellText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim().replace("\n", " ");
    }

    private byte[] renderSlideToPng(SlideShow<?, ?> slideShow, Slide<?, ?> slide) throws IOException {
        Dimension pageSize = slideShow.getPageSize();
        BufferedImage image = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, pageSize.width, pageSize.height);
            slide.draw(graphics);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

}
