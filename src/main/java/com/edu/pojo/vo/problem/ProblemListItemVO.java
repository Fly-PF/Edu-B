package com.edu.pojo.vo.problem;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProblemListItemVO {
    private Long id;
    private String title;
    private String subject;
    private String category;
    private String difficulty;
    private List<String> tags;
    private String intro;
    private String mastery;
    private Integer version;
}
