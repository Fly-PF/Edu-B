package com.edu.pojo.dto.personnel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePersonnelStatusRequest {
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值错误")
    @Max(value = 1, message = "状态值错误")
    private Integer status;
}
