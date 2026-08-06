package com.edu.pojo.dto.safety;

import com.edu.pojo.enums.safety.SafetyReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyReviewActionRequest implements Serializable {
    private String comment;
    private String reviewRemark;
    private String decision;
    private SafetyReviewStatus reviewStatus;

    public String resolvedComment() {
        if (comment != null && !comment.isBlank()) {
            return comment.trim();
        }
        if (reviewRemark != null && !reviewRemark.isBlank()) {
            return reviewRemark.trim();
        }
        return null;
    }
}
