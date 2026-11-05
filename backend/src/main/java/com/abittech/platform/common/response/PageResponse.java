package com.abittech.platform.common.response;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for paged lists (instead of exposing Spring's {@code Page} internals).
 *
 * @param page       zero-based page index
 * @param size       requested page size
 * @param totalItems number of items over all pages
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
