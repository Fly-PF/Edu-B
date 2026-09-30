package com.edu.pojo.vo.problem;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProblemDetailVO {
    private Long id;
    private String title;
    private Long subjectId;
    private Long categoryId;
    private String subject;
    private String category;
    private String difficulty;
    private Long creatorId;
    private String creatorNickname;
    private List<String> tags;
    private String intro;
    private Integer version;
    private String standardAnswer;
    private String answerAnalysis;
    private Map<String, Object> definition;
    private List<ProblemImageVO> images;
}
