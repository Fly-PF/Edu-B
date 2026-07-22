package com.edu.util;

import com.edu.common.dto.RagTextChunkDTO;
import com.edu.util.abstracts.AbstractTikaTextExtractUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class TxtTextExtractUtil extends AbstractTikaTextExtractUtil {
    public List<RagTextChunkDTO> extract(InputStream inputStream) {
        return extractParagraphChunks(log, "TXT", inputStream);
    }
}
