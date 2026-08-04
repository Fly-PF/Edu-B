package com.edu.pojo.dto.teacherai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LessonPlanGenerateRequest {
    @Positive(message = "课程ID必须大于0")
    private Long courseId;

    @NotBlank(message = "课题名称不能为空")
    @Size(max = 200, message = "课题名称不能超过200个字符")
    private String topic;

    @NotBlank(message = "学段不能为空")
    @Size(max = 50, message = "学段不能超过50个字符")
    private String grade;

    @NotNull(message = "课时分钟数不能为空")
    @Min(value = 20, message = "课时不能少于20分钟")
    @Max(value = 240, message = "课时不能超过240分钟")
    private Integer durationMinutes;

    @NotBlank(message = "教学目标不能为空")
    @Size(max = 2000, message = "教学目标不能超过2000个字符")
    private String objectives;

    @NotBlank(message = "难度不能为空")
    @Size(max = 50, message = "难度不能超过50个字符")
    private String difficulty;

    @Size(max = 2000, message = "补充要求不能超过2000个字符")
    private String requirements;
}
