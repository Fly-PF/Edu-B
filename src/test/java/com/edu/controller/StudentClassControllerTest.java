package com.edu.controller;

import com.edu.pojo.dto.student.StudentJoinClassRequest;
import com.edu.pojo.dto.student.StudentJoinedClassDTO;
import com.edu.service.StudentClassService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentClassControllerTest {
    private MockMvc mockMvc;
    private StudentClassService studentClassService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        studentClassService = mock(StudentClassService.class);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentClassController(studentClassService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void shouldJoinClass() throws Exception {
        when(studentClassService.joinClass(any(StudentJoinClassRequest.class))).thenReturn(
                StudentJoinedClassDTO.builder().classId(11L).className("高一AI创新班").teacherName("王老师").build()
        );

        String body = """
                {
                  "classCode": "AI202607"
                }
                """;

        mockMvc.perform(post("/api/student/classes/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.classId").value(11L))
                .andExpect(jsonPath("$.data.className").value("高一AI创新班"));
    }

    @Test
    void shouldLeaveClass() throws Exception {
        doNothing().when(studentClassService).leaveClass(12L);

        mockMvc.perform(delete("/api/student/classes/12/leave"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldListJoinedClasses() throws Exception {
        when(studentClassService.listJoinedClasses()).thenReturn(List.of(
                StudentJoinedClassDTO.builder().classId(13L).className("高二数据科学班").teacherName("李老师").build()
        ));

        mockMvc.perform(get("/api/student/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].classId").value(13L))
                .andExpect(jsonPath("$.data[0].className").value("高二数据科学班"));
    }
}
