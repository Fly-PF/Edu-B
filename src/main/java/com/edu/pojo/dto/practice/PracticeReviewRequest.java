package com.edu.pojo.dto.practice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class PracticeReviewRequest {
    @NotNull(message = "请输入评分")
    @Min(value = 0, message = "评分不能小于 0")
    @Max(value = 100, message = "评分不能超过 100")
    private Integer score;
    @NotBlank(message = "请填写整份练习的总反馈")
    @Size(max = 1000, message = "评语不能超过 1000 字")
    private String feedback;
    @Valid
    @NotNull(message = "请完成每道开放题的批改")
    private List<QuestionReview> questionReviews;

    @Data
    public static class QuestionReview {
        @NotNull(message = "开放题编号不能为空")
        private Long questionId;
        @NotNull(message = "请填写开放题得分")
        @Min(value = 0, message = "开放题得分不能小于 0")
        @Max(value = 100, message = "开放题得分不能超过 100")
        private Integer score;
        @NotBlank(message = "请填写开放题反馈")
        @Size(max = 500, message = "单题反馈不能超过 500 字")
        private String feedback;
    }
}
