package com.edu.pojo.vo.personnel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<T> records;
}
