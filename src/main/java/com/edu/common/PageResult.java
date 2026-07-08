package com.edu.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<T> records;

    public static <T> PageResult<T> of(long total, PageQuery pageQuery, List<T> records) {
        return PageResult.<T>builder()
                .total(total)
                .pageNum(pageQuery.getPageNum())
                .pageSize(pageQuery.getPageSize())
                .records(records)
                .build();
    }
}
