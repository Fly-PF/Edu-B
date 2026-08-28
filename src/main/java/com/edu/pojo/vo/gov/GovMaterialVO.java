package com.edu.pojo.vo.gov;

import com.edu.pojo.dto.gov.GovMaterialLinkDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GovMaterialVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String description;
    private Integer materialType;
    private List<GovMaterialLinkDTO> links;
    private String fileName;
    private String fileUrl;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
