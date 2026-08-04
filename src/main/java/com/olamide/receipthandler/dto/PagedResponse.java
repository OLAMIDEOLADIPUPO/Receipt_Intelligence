package com.olamide.receipthandler.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "A single page of results plus the metadata needed to walk the remaining pages.")
public record PagedResponse<T>(
        @Schema(description = "The results on this page") List<T> content,
        @Schema(description = "Zero-based page number", example = "0") int page,
        @Schema(description = "Number of results requested per page", example = "20") int size,
        @Schema(description = "Total matching results across all pages", example = "137") long totalElements,
        @Schema(description = "Total number of pages", example = "7") int totalPages,
        @Schema(description = "True when this is the last page", example = "false") boolean last
) {
    /** Build a response from a page whose content is already the desired type. */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return of(page, page.getContent());
    }

    /** Build a response from a source page but with separately-mapped content (e.g. entity -> DTO). */
    public static <T> PagedResponse<T> of(Page<?> source, List<T> content) {
        return new PagedResponse<>(
                content,
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isLast()
        );
    }
}
