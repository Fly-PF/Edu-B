package com.edu.util;

import com.edu.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
public class WordHtmlPreviewUtil {
    public String renderHtml(InputStream inputStream, String extension) {
        byte[] fileBytes;
        try {
            fileBytes = toByteArray(inputStream);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "Word文件读取失败");
        }

        return switch (normalizeExtension(extension)) {
            case "docx" -> renderDocx(fileBytes);
            case "doc" -> renderDoc(fileBytes);
            default -> throw new BaseException(HttpStatus.BAD_REQUEST, "暂不支持该Word格式预览");
        };
    }

    private String renderDocx(byte[] fileBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            StringBuilder html = new StringBuilder();
            appendDocxBody(document.getBodyElements(), html);
            return html.toString();
        } catch (OLE2NotOfficeXmlFileException ex) {
            log.warn("The DOCX file content is OLE2, using DOC html preview instead");
            return renderDoc(fileBytes);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "DOCX预览失败");
        }
    }

    private String renderDoc(byte[] fileBytes) {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileBytes))) {
            PicturesTable picturesTable = document.getPicturesTable();
            Range range = document.getRange();
            Set<Integer> pictureOffsets = new HashSet<>();
            StringBuilder html = new StringBuilder();
            for (int i = 0; i < range.numParagraphs(); i++) {
                Paragraph paragraph = range.getParagraph(i);
                StringBuilder paragraphHtml = new StringBuilder();
                for (int j = 0; j < paragraph.numCharacterRuns(); j++) {
                    CharacterRun characterRun = paragraph.getCharacterRun(j);
                    if (picturesTable.hasPicture(characterRun) || picturesTable.hasEscherPicture(characterRun)) {
                        Picture picture = picturesTable.extractPicture(characterRun, false);
                        if (picture != null && pictureOffsets.add(picture.getStartOffset())) {
                            appendImage(paragraphHtml, picture.getContent(), picture.suggestFileExtension());
                        }
                        continue;
                    }
                    appendEscapedText(paragraphHtml, characterRun.text());
                }
                appendParagraph(html, paragraphHtml.toString());
            }
            return html.toString();
        } catch (OfficeXmlFileException ex) {
            log.warn("The DOC file content is OOXML, using DOCX html preview instead");
            return renderDocx(fileBytes);
        } catch (Exception ex) {
            throw new BaseException(HttpStatus.INTERNAL_SERVER_ERROR, "DOC预览失败");
        }
    }

    private void appendDocxBody(List<IBodyElement> bodyElements, StringBuilder html) {
        for (IBodyElement bodyElement : bodyElements) {
            switch (bodyElement.getElementType()) {
                case PARAGRAPH -> appendDocxParagraph((XWPFParagraph) bodyElement, html);
                case TABLE -> appendDocxTable((XWPFTable) bodyElement, html);
                default -> {
                }
            }
        }
    }

    private void appendDocxParagraph(XWPFParagraph paragraph, StringBuilder html) {
        StringBuilder paragraphHtml = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            appendEscapedText(paragraphHtml, run.text());
            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                XWPFPictureData pictureData = picture.getPictureData();
                if (pictureData != null) {
                    appendImage(paragraphHtml, pictureData.getData(), pictureData.suggestFileExtension());
                }
            }
        }
        appendParagraph(html, paragraphHtml.toString());
    }

    private void appendDocxTable(XWPFTable table, StringBuilder html) {
        html.append("<table><tbody>");
        for (XWPFTableRow row : table.getRows()) {
            html.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                html.append("<td>");
                appendDocxBody(cell.getBodyElements(), html);
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendParagraph(StringBuilder html, String paragraphHtml) {
        if (!StringUtils.hasText(stripTags(paragraphHtml))) {
            return;
        }
        html.append("<p>").append(paragraphHtml).append("</p>");
    }

    private void appendEscapedText(StringBuilder html, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        html.append(escapeHtml(text).replace("\r\n", "\n").replace('\r', '\n').replace("\n", "<br>"));
    }

    private void appendImage(StringBuilder html, byte[] bytes, String extension) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        html.append("<img src=\"")
                .append(toDataUrl(bytes, extension))
                .append("\" alt=\"文档图片\">");
    }

    private String toDataUrl(byte[] bytes, String extension) {
        return "data:" + imageContentType(extension) + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private String imageContentType(String extension) {
        return switch (normalizeExtension(extension)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String stripTags(String html) {
        return html == null ? "" : html.replaceAll("<[^>]+>", "").trim();
    }

    private byte[] toByteArray(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private String normalizeExtension(String extension) {
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }
}
