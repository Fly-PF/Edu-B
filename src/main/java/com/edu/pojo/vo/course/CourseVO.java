package com.edu.pojo.vo.course;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseVO {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private String title;
    private String courseName;
    private String description;
    private String intro;
    private List<String> tags;
    private String coverUrl;
    private String cover;
    private String grade;
    private Integer difficulty;
    private Integer courseType;
    private Integer totalDuration;
    private Integer totalChapter;
    private Long resourceCount;
    private String seriesName;
    private Integer seriesOrder;
    private Integer likeCount;
    private LocalDateTime publishedTime;
    private String status;
    private Boolean publicCourse;
    private Integer isPublic;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
