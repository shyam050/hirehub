package com.hirehub.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Safe pagination helper with max page size enforcement.
 * Prevents clients from requesting unbounded records.
 */
public final class PaginationHelper {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private PaginationHelper() {}

    /**
     * Create a safe Pageable from user-supplied parameters.
     * Clamps page to >= 0, size to [1, MAX_PAGE_SIZE].
     */
    public static Pageable of(int page, int size) {
        return of(page, size, Sort.unsorted());
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, sort);
    }

    /**
     * Resolve sort from a string field name. Defaults to "createdAt" descending.
     */
    public static Sort resolveSort(String field, String direction) {
        if (field == null || field.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Sort.Direction dir = Sort.Direction.fromString(direction != null ? direction.toUpperCase() : "DESC");
        return Sort.by(dir, field);
    }
}
