package com.edu.controller;

import com.edu.pojo.dto.course.CourseCreateRequest;
import com.edu.pojo.dto.course.CourseUpdateRequest;
import com.edu.pojo.vo.course.CourseVO;
import com.edu.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherCourseControllerTest {
    private MockMvc mockMvc;
    private CourseService courseService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        courseService = mock(CourseService.class);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new TeacherCourseController(courseService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void shouldListTeacherCourses() throws Exception {
        when(courseService.listTeacherCourses(null, null)).thenReturn(List.of(
                CourseVO.builder().id(1L).title("AI基础").courseName("AI基础").build()
        ));

        mockMvc.perform(get("/api/teacher/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("AI基础"));
    }

    @Test
    void shouldCreateCourse() throws Exception {
        when(courseService.createCourse(any(CourseCreateRequest.class))).thenReturn(
                CourseVO.builder().id(2L).title("Python项目").courseName("Python项目").build()
        );

        String body = """
                {
                  "title": "Python项目",
                  "description": "入门课程",
                  "tags": ["Python"],
                  "coverUrl": "",
                  "grade": "高中",
                  "difficulty": 1,
                  "courseType": 2
                }
                """;

        mockMvc.perform(post("/api/teacher/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.title").value("Python项目"));
    }

    @Test
    void shouldUpdateCourseWithPut() throws Exception {
        when(courseService.updateCourse(eq(3L), any(CourseUpdateRequest.class))).thenReturn(
                CourseVO.builder().id(3L).title("更新后的课程").courseName("更新后的课程").build()
        );

        String body = """
                {
                  "title": "更新后的课程",
                  "description": "更新描述",
                  "grade": "高中",
                  "difficulty": 2,
                  "courseType": 1
                }
                """;

        mockMvc.perform(put("/api/teacher/courses/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3L))
                .andExpect(jsonPath("$.data.title").value("更新后的课程"));
    }

    @Test
    void shouldDeleteCourse() throws Exception {
        doNothing().when(courseService).deleteCourse(4L);

        mockMvc.perform(delete("/api/teacher/courses/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
