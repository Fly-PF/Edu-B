package com.edu.util;

import com.edu.common.dto.RagTextChunkDTO;
import com.edu.util.abstracts.AbstractTikaTextExtractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTextExtractUtil extends AbstractTikaTextExtractUtil {
    private static final int MIN_TEXT_CHAR_COUNT = 400;
    private static final float PDF_RENDER_DPI = 200F;
    private static final String PDF_FALLBACK_PREFIX = "下面是根据原始内容转换的Markdown格式的内容，可能会与原始内容有所差异，请注意：\n";

    private final ImageTextExtractUtil imageTextExtractUtil;

    public List<RagTextChunkDTO> extract(InputStream inputStream) {
        validateInputStream(inputStream, "PDF");
        try (PDDocument document = Loader.loadPDF(toByteArray(inputStream))) {
            List<RagTextChunkDTO> chunks = new ArrayList<>();
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new LayoutAwarePdfTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String text = extractPageText(document, stripper, renderer, pageNum);
                chunks.add(logPage(log, "PDF", pageNum, totalPages, text));
            }
            return chunks;
        } catch (Exception ex) {
            throwExtractException(log, "PDF", ex);
            return List.of();
        }
    }

    private String extractPageText(PDDocument document, PDFTextStripper stripper, PDFRenderer renderer, int pageNum)
            throws IOException {
        String text = stripper.getText(document);
        if (countNonWhitespaceChars(text) >= MIN_TEXT_CHAR_COUNT) {
            return "## 第 " + pageNum + " 页\n\n" + text;
        }
        return imageTextExtractUtil.extract(new ByteArrayInputStream(renderPageToPng(renderer, pageNum)), false,
                PDF_FALLBACK_PREFIX);
    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private byte[] renderPageToPng(PDFRenderer renderer, int pageNum) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageNum - 1, PDF_RENDER_DPI, ImageType.RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static class LayoutAwarePdfTextStripper extends PDFTextStripper {
        LayoutAwarePdfTextStripper() throws IOException {
            setSortByPosition(true);
            setShouldSeparateByBeads(false);
            setLineSeparator(System.lineSeparator());
            setWordSeparator(" ");
            setSpacingTolerance(0.5F);
            setAverageCharTolerance(0.3F);
            setAddMoreFormatting(true);
        }
    }

}
