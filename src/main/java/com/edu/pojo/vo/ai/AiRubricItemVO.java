package com.edu.pojo.vo.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRubricItemVO {
    private String criterion;
    private String excellent;
    private String good;
    private String pass;
    private String needsImprovement;
}
