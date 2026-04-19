package com.venkatesh.userservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;

    private int page;        // current page number (0-based)
    private int size;        // page size requested
    private long totalElements;
    private int totalPages;
    private boolean last;
}

