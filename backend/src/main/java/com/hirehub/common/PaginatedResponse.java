package com.hirehub.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standardized paginated response used across all collection endpoints.
 * Returns a consistent envelope: content, page, size, totalElements, totalPages, first, last.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public PaginatedResponse(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public static <T> PaginatedResponse<T> of(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static <T> PaginatedResponse<T> empty(int page, int size) {
        return new PaginatedResponse<>(
                List.of(), page, size, 0L, 0, true, true
        );
    }
}
