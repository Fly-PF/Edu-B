package com.edu.pojo.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompanionSessionVO {
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
}
