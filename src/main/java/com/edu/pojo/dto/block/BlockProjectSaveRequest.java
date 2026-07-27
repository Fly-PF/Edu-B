package com.edu.pojo.dto.block;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlockProjectSaveRequest {
    @NotBlank
    @Size(max = 100)
    private String title;
    @Size(max = 500)
    private String description;
    @NotBlank
    @Size(max = 1000000)
    private String workspaceJson;
    @NotBlank
    @Size(max = 200000)
    private String stageJson;
    @Size(max = 1000000)
    private String thumbnailData;
}
