package com.edu.pojo.vo.gov;

import com.edu.pojo.dto.gov.GovQuestionContentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovQuestionAdminVO {
    private Long id;
    private String subject;
    private String questionType;
    private Integer difficulty;
    private Integer examYear;
    private String sourceType;
    private GovQuestionContentDTO content;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

