package com.edu.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageQuery {
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private Integer pageNum;
    private Integer pageSize;

    public static PageQuery of(Integer pageNum, Integer pageSize) {
        int normalizedPageNum = pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
        normalizedPageSize = Math.min(normalizedPageSize, MAX_PAGE_SIZE);
        return new PageQuery(normalizedPageNum, normalizedPageSize);
    }

    public long offset() {
        return (long) (pageNum - 1) * pageSize;
    }
}
