package com.edu.pojo.dto.safety;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagEvidenceReferenceDTO implements Serializable {
    private String title;
    private String snippet;
    private String sourceId;
    private Double score;
}
