package com.edu.pojo.vo.gov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovNewsListItemVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String coverUrl;
    private Integer isTop;
    private Integer status;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
