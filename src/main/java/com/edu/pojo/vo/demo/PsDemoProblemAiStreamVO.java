package com.edu.pojo.vo.demo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PsDemoProblemAiStreamVO {
    private String status;
    private String messageId;
    private String content;
}
