package com.edu.pojo.dto.gov;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GovMaterialLinkDTO {
    @NotBlank(message = "平台标识不能为空")
    @Size(max = 20, message = "平台标识不能超过20个字符")
    private String platform;

    @NotBlank(message = "网盘链接不能为空")
    @Size(max = 500, message = "网盘链接不能超过500个字符")
    private String url;

    @Size(max = 20, message = "提取码不能超过20个字符")
    private String accessCode;
}
