package com.edu.util;

import com.edu.common.dto.RagTextChunkDTO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VttTextExtractUtilTest {
    @Test
    void extractsCaptionTextWithoutWebVttMetadata() {
        String vtt = "WEBVTT\n\n"
                + "1\n"
                + "00:00.000 --> 00:02.000\n"
                + "<v Teacher>人工智能是让计算机完成复杂任务。\n\n"
                + "00:02.000 --> 00:04.000\n"
                + "第二句字幕。\n";

        List<RagTextChunkDTO> chunks = new VttTextExtractUtil().extract(
                new ByteArrayInputStream(vtt.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, chunks.size());
        assertEquals("字幕 1/1", chunks.getFirst().getSourceInfo());
        assertEquals("人工智能是让计算机完成复杂任务。" + System.lineSeparator() + "第二句字幕。",
                chunks.getFirst().getContent());
        assertFalse(chunks.getFirst().getContent().contains("00:00.000"));
        assertFalse(chunks.getFirst().getContent().contains("WEBVTT"));
    }
}
