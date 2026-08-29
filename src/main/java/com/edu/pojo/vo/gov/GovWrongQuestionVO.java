package com.edu.pojo.vo.gov;

import com.edu.pojo.dto.gov.GovQuestionContentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovWrongQuestionVO {
    private Long questionId;
    private Integer questionOrder;
    private String questionType;
    private String subject;
    private Integer difficulty;
    private GovQuestionContentDTO content;
    private List<String> selectedAnswers;
    private List<String> correctAnswers;
    private String analysis;
}

