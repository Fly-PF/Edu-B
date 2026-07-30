package com.edu.pojo.dto.practice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PracticeSubmitRequest {
    @Valid
    @NotEmpty(message = "请完成所有题目后再提交")
    private List<PracticeAnswerRequest> answers;
}
