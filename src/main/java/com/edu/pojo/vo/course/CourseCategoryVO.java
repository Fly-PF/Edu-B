package com.edu.pojo.vo.course;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CourseCategoryVO {
    private Long id;
    private String name;
    private Integer sortOrder;
    private List<String> tags;
    private Boolean matchAll;
    private List<CourseVO> courses;
    private Long totalCourses;
}
