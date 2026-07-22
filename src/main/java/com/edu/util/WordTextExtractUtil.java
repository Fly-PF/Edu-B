package com.edu.util;

import com.edu.common.dto.RagTextChunkDTO;
import com.edu.util.abstracts.AbstractTikaTextExtractUtil;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WordTextExtractUtil extends AbstractTikaTextExtractUtil {
    private static final int SEGMENT_CHAR_LIMIT = 1000;

    private final ImageTextExtractUtil imageTextExtractUtil;

    public List<RagTextChunkDTO> extract(InputStream inputStream, String extension) {
        validateInputStream(inputStream, "WORD");
        byte[] fileBytes;
        try {
            fileBytes = toByteArray(inputStream);
        } catch (Exception ex) {
            throwExtractException(log, "WORD", ex);
            return List.of();
        }

        return switch (normalizeExtension(extension)) {
            case "docx" -> extractDocx(fileBytes);
            case "doc" -> extractDoc(fileBytes);
            default -> {
                throwExtractException(log, "WORD", new IllegalArgumentException("Unsupported word extension"));
                yield List.of();
            }
        };
    }

    private List<RagTextChunkDTO> extractDocx(byte[] fileBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            List<WordElement> elements = collectDocxElements(document.getBodyElements());
            WordExtractResult result = buildWordExtractResult("DOCX", elements);
            List<RagTextChunkDTO> chunks = logWordSegments("DOCX", result.parts());
            logImages(result.imageLogs());
            return chunks;
        } catch (OLE2NotOfficeXmlFileException ex) {
            log.warn("The DOCX file content is OLE2, using DOC extractor instead");
            return extractDoc(fileBytes);
        } catch (Exception ex) {
            throwExtractException(log, "DOCX", ex);
            return List.of();
        }
    }

    private List<RagTextChunkDTO> extractDoc(byte[] fileBytes) {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileBytes))) {
            List<WordElement> elements = collectDocElements(document);
            WordExtractResult result = buildWordExtractResult("DOC", elements);
            List<RagTextChunkDTO> chunks = logWordSegments("DOC", result.parts());
            logImages(result.imageLogs());
            return chunks;
        } catch (OfficeXmlFileException ex) {
            log.warn("The DOC file content is OOXML, using DOCX extractor instead");
            return extractDocx(fileBytes);
        } catch (Exception ex) {
            throwExtractException(log, "DOC", ex);
            return List.of();
        }
    }

    private List<WordElement> collectDocxElements(List<IBodyElement> bodyElements) {
        List<WordElement> elements = new ArrayList<>();
        for (IBodyElement bodyElement : bodyElements) {
            switch (bodyElement.getElementType()) {
                case PARAGRAPH -> addDocxParagraphElement((XWPFParagraph) bodyElement, elements);
                case TABLE -> collectDocxTableElements((XWPFTable) bodyElement, elements);
                default -> {
                }
            }
        }
        return elements;
    }

    private void collectDocxTableElements(XWPFTable table, List<WordElement> elements) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                elements.addAll(collectDocxElements(cell.getBodyElements()));
            }
        }
    }

    private void addDocxParagraphElement(XWPFParagraph paragraph, List<WordElement> elements) {
        List<WordImage> images = new ArrayList<>();
        for (XWPFRun run : paragraph.getRuns()) {
            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                XWPFPictureData pictureData = picture.getPictureData();
                if (pictureData != null) {
                    images.add(new WordImage(pictureData.getData(), pictureData.suggestFileExtension()));
                }
            }
        }

        String text = normalizeText(paragraph.getText());
        if (StringUtils.hasText(text) || !images.isEmpty()) {
            elements.add(new WordElement(text, images));
        }
    }

    private List<WordElement> collectDocElements(HWPFDocument document) {
        PicturesTable picturesTable = document.getPicturesTable();
        Range range = document.getRange();
        List<WordElement> elements = new ArrayList<>();
        Set<Integer> pictureOffsets = new HashSet<>();
        for (int i = 0; i < range.numParagraphs(); i++) {
            Paragraph paragraph = range.getParagraph(i);
            List<WordImage> images = new ArrayList<>();
            for (int j = 0; j < paragraph.numCharacterRuns(); j++) {
                CharacterRun characterRun = paragraph.getCharacterRun(j);
                if (!picturesTable.hasPicture(characterRun) && !picturesTable.hasEscherPicture(characterRun)) {
                    continue;
                }
                Picture picture = picturesTable.extractPicture(characterRun, false);
                if (picture != null && pictureOffsets.add(picture.getStartOffset())) {
                    images.add(new WordImage(picture.getContent(), picture.suggestFileExtension()));
                }
            }
            String text = normalizeText(paragraph.text());
            if (StringUtils.hasText(text) || !images.isEmpty()) {
                elements.add(new WordElement(text, images));
            }
        }
        return elements;
    }

    private WordExtractResult buildWordExtractResult(String fileTypeName, List<WordElement> elements) throws Exception {
        List<WordTextPart> parts = new ArrayList<>();
        List<WordImageLog> imageLogs = new ArrayList<>();
        int imageNum = 1;
        for (int i = 0; i < elements.size(); i++) {
            imageNum = appendWordElement(fileTypeName, elements, i, parts, imageLogs, imageNum);
        }
        return new WordExtractResult(parts, imageLogs);
    }

    private int appendWordElement(String fileTypeName,
                                  List<WordElement> elements,
                                  int index,
                                  List<WordTextPart> parts,
                                  List<WordImageLog> imageLogs,
                                  int imageNum) throws Exception {
        WordElement element = elements.get(index);
        String paragraphText = normalizeText(element.text());
        if (StringUtils.hasText(paragraphText)) {
            parts.add(new WordTextPart(paragraphText, index + 1, false));
        }
        for (WordImage image : element.images()) {
            parts.add(new WordTextPart(imageTextExtractUtil.extract(new ByteArrayInputStream(image.data()), false),
                    index + 1, true));
            Path imagePath = saveExtractedImage(image.data(), imageNum, image.extension());
            imageLogs.add(new WordImageLog(fileTypeName, imageNum, imagePath, "paragraph " + (index + 1),
                    previousText(elements, index), nextText(elements, index)));
            imageNum++;
        }
        return imageNum;
    }

    private List<RagTextChunkDTO> logWordSegments(String fileTypeName, List<WordTextPart> parts) {
        List<TextSegment> segments = buildWordSegments(parts);
        List<RagTextChunkDTO> chunks = new ArrayList<>();
        int totalSegments = segments.size();
        for (int i = 0; i < totalSegments; i++) {
            TextSegment segment = segments.get(i);
            log.info("Extracted {} segment {}/{}, paragraphs {}-{}, content: {}",
                    fileTypeName, i + 1, totalSegments, segment.startParagraph(), segment.endParagraph(), segment.text());
            chunks.add(new RagTextChunkDTO("paragraphs " + segment.startParagraph() + "-" + segment.endParagraph()
                    + "/" + paragraphCount(parts), segment.text()));
        }
        return chunks;
    }

    private List<TextSegment> buildWordSegments(List<WordTextPart> parts) {
        List<TextSegment> segments = new ArrayList<>();
        if (parts == null || parts.isEmpty()) {
            return segments;
        }

        StringBuilder segmentText = new StringBuilder();
        int segmentChars = 0;
        int startParagraph = 0;
        int endParagraph = 0;

        for (WordTextPart part : parts) {
            String text = part.text();
            if (!StringUtils.hasText(text)) {
                continue;
            }

            if (part.image() && segmentText.length() > 0 && segmentChars >= SEGMENT_CHAR_LIMIT) {
                segments.add(new TextSegment(startParagraph, endParagraph, segmentText.toString()));
                segmentText.setLength(0);
                segmentChars = 0;
            }

            if (segmentText.isEmpty()) {
                startParagraph = part.paragraphIndex();
            } else {
                segmentText.append(System.lineSeparator());
            }
            segmentText.append(text);
            segmentChars += countNonWhitespaceChars(text);
            endParagraph = part.paragraphIndex();

            if (segmentChars >= SEGMENT_CHAR_LIMIT) {
                segments.add(new TextSegment(startParagraph, endParagraph, segmentText.toString()));
                segmentText.setLength(0);
                segmentChars = 0;
            }
        }

        if (segmentText.length() > 0) {
            segments.add(new TextSegment(startParagraph, endParagraph, segmentText.toString()));
        }
        return segments;
    }

    private int paragraphCount(List<WordTextPart> parts) {
        int total = 0;
        int lastParagraphIndex = 0;
        for (WordTextPart part : parts) {
            if (part.paragraphIndex() != lastParagraphIndex) {
                total++;
                lastParagraphIndex = part.paragraphIndex();
            }
        }
        return total;
    }

    private void logImages(List<WordImageLog> imageLogs) {
        for (WordImageLog imageLog : imageLogs) {
            logImage(log, imageLog.fileTypeName(), imageLog.imageNum(), imageLog.imagePath(), imageLog.location(),
                    imageLog.previousText(), imageLog.nextText());
        }
    }

    private String previousText(List<WordElement> elements, int index) {
        for (int i = index - 1; i >= 0; i--) {
            if (StringUtils.hasText(elements.get(i).text())) {
                return elements.get(i).text();
            }
        }
        return "";
    }

    private String nextText(List<WordElement> elements, int index) {
        for (int i = index + 1; i < elements.size(); i++) {
            if (StringUtils.hasText(elements.get(i).text())) {
                return elements.get(i).text();
            }
        }
        return "";
    }

    private byte[] toByteArray(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private String normalizeExtension(String extension) {
        return extension == null ? "" : extension.toLowerCase();
    }

    private record WordElement(String text, List<WordImage> images) {
    }

    private record WordImage(byte[] data, String extension) {
    }

    private record WordTextPart(String text, int paragraphIndex, boolean image) {
    }

    private record WordExtractResult(List<WordTextPart> parts, List<WordImageLog> imageLogs) {
    }

    private record WordImageLog(String fileTypeName, int imageNum, Path imagePath, String location,
                                String previousText, String nextText) {
    }

    private record TextSegment(int startParagraph, int endParagraph, String text) {
    }
}
