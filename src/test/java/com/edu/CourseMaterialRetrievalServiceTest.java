package com.edu;

import com.edu.pojo.vo.ai.AiCompanionMaterialExcerpt;
import com.edu.pojo.vo.course.ResourceVO;
import com.edu.service.CourseResourceStorageService;
import com.edu.service.impl.CourseMaterialRetrievalServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseMaterialRetrievalServiceTest {
    @Test
    void retrievesTheRelevantPdfPageForAQuestion() throws IOException {
        CourseResourceStorageService storageService = mock(CourseResourceStorageService.class);
        when(storageService.readLocalBytes("course/4/test.pdf", 16L * 1024 * 1024))
                .thenReturn(createPdf("Training data needs clear labels. Keep test samples separate."));
        CourseMaterialRetrievalServiceImpl service = new CourseMaterialRetrievalServiceImpl(storageService);
        ResourceVO resource = ResourceVO.builder()
                .name("Machine learning lab handout")
                .type(2)
                .storedUrl("course/4/test.pdf")
                .build();

        List<AiCompanionMaterialExcerpt> results = service.retrieve(List.of(resource), "How should I prepare training data?");

        assertFalse(results.isEmpty());
        assertEquals("Machine learning lab handout", results.getFirst().resourceName());
        assertEquals(1, results.getFirst().pageNumber());
    }

    private byte[] createPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 700);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
