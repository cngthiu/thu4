package com.example.library.dto;

import java.util.List;

public record PagedResult<T>(List<T> items, long total, int page, int size) {
    public long totalPages() {
        if (size <= 0) {
            return 1;
        }
        long pages = total / size;
        if (total % size != 0) {
            pages += 1;
        }
        return Math.max(pages, 1);
    }

    public int firstItemIndex() {
        return page * size;
    }
}
