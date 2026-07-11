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
    private String description;
    private List<String> tags;
    private String coverUrl;
    private String grade;
    private Integer difficulty;
    private Integer courseType;
    private Integer totalDuration;
    private Integer totalChapter;
    private Long resourceCount;
    private String status;
    private Boolean publicCourse;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
