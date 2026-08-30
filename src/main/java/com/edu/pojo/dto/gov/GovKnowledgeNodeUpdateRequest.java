package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovKnowledgeNodeUpdateRequest {
    @Size(max = 30, message = "科目长度不能超过30个字符")
    private String subject;

    @Min(value = 0, message = "父节点ID不能小于0")
    private Long parentId;

    @Size(max = 20, message = "节点类型长度不能超过20个字符")
    private String nodeType;

    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    @Size(max = 50000, message = "正文内容不能超过50000个字符")
    private String contentMd;

    @Min(value = 0, message = "排序不能小于0")
    private Integer sortOrder;

    @Min(value = 0, message = "状态不正确")
    @Max(value = 1, message = "状态不正确")
    private Integer status;
}
