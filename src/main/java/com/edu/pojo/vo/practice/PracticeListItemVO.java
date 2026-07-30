package com.edu.pojo.vo.practice;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PracticeListItemVO {
    private Long id;
    private Long courseId;
    private String courseName;
    private String title;
    private String intro;
    private Integer totalScore;
    private Integer questionCount;
    private String status;
    private Integer score;
    private LocalDateTime submitTime;
}
