package com.authshield360.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Serializable page wrapper (avoids leaking Spring Page internals). */
public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResult<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
