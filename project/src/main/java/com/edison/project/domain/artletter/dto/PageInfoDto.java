package com.edison.project.domain.artletter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageInfoDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
