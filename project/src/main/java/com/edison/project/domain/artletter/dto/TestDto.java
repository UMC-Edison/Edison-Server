package com.edison.project.domain.artletter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TestDto {
    private boolean isSuccess;
    private int code;
    private String message;
    private PageInfoDto pageInfo;
    private List<ArtletterDto> result;
}
