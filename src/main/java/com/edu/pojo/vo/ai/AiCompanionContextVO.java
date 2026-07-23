package com.edu.pojo.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCompanionContextVO {
    private Long courseId;
    private String courseTitle;
    private String courseIntro;
    private Long chapterId;
    private String chapterTitle;
    private Long resourceId;
    private String resourceName;
    private Integer resourceType;
    private Integer progress;
    private Integer finishStatus;
    private Integer completedChapterCount;
    private Integer totalChapterCount;
    private List<String> chapterTitles;
    private List<String> resourceNames;
    private Long nextChapterId;
    private String nextChapterTitle;
}
