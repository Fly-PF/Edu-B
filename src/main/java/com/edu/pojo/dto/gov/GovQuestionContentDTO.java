package com.edu.pojo.dto.gov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovQuestionContentDTO {
    private String stem;
    private String material;

    @Builder.Default
    private List<Option> options = List.of();

    @Builder.Default
    private List<String> answer = List.of();

    private String analysis;

    @Builder.Default
    private List<String> tags = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String key;
        private String content;
    }
}

