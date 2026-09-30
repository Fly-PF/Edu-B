package com.edu.pojo.vo.gov;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GovPracticeAiStreamVO {
    private String status;
    private String messageId;
    private String content;
}
