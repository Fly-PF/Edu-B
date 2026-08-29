package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovNewsUpdateRequest {
    @NotNull(message = "资讯分类不能为空")
    @Positive(message = "资讯分类不正确")
    private Long categoryId;
    @NotBlank(message = "资讯标题不能为空")
    @Size(max = 200, message = "资讯标题不能超过200个字符")
    private String title;
    @Size(max = 500, message = "资讯摘要不能超过500个字符")
    private String summary;
    @NotBlank(message = "资讯正文不能为空")
    @Size(max = 100000, message = "资讯正文不能超过100000个字符")
    private String contentMd;
    @Size(max = 500, message = "封面地址不能超过500个字符")
    private String coverUrl;
    @NotNull(message = "置顶状态不能为空")
    @Min(value = 0, message = "置顶状态不正确")
    @Max(value = 1, message = "置顶状态不正确")
    private Integer isTop;
}
