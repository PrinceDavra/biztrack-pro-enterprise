package com.biztrackpro.dto;

import java.util.List;

/**
 * Generic pagination envelope for list endpoints.
 */
public class PageDTO<T> {

    public List<T> items;
    public long total;
    public int page;
    public int size;
    public int totalPages;

    public PageDTO() {
    }

    public PageDTO(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }
}
