package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovKnowledgeAnnotationSaveRequest {
    @NotBlank(message = "请选择正文段落")
    @Size(max = 100, message = "段落标识长度不能超过100个字符")
    private String sectionKey;

    @NotBlank(message = "请选择正文段落")
    @Size(max = 200, message = "段落标题长度不能超过200个字符")
    private String sectionTitle;

    @NotNull(message = "请选择正文内容")
    @Min(value = 0, message = "起始位置不正确")
    private Integer startOffset;

    @NotNull(message = "请选择正文内容")
    @Min(value = 1, message = "结束位置不正确")
    private Integer endOffset;

    @NotBlank(message = "请选择正文内容")
    @Size(max = 2000, message = "选中文本长度不能超过2000个字符")
    private String selectedText;

    @NotBlank(message = "标注内容不能为空")
    @Size(max = 5000, message = "标注内容长度不能超过5000个字符")
    private String noteContent;

    @Size(max = 20, message = "颜色值长度不能超过20个字符")
    private String color;
}
