package com.edu.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStudentCourseStudyRecordDTO {
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseName;
    private Integer progress;
    private Integer studyDuration;
    private Integer studyStatus;
    private List<ChapterStudyRecord> chapters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterStudyRecord {
        private Long chapterId;
        private String chapterName;
        private Integer sort;
        private Integer duration;
        private Long resourceId;
        private Integer progress;
        private Integer studyDuration;
        private Integer finishStatus;
        private String lastStudyTime;
    }
}
