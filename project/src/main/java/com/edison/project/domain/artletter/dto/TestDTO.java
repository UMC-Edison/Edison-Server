package com.edison.project.domain.artletter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TestDTO {
    private boolean isSuccess;
    private int code;
    private String message;
    private PageInfoDTO pageInfo;
    private List<ArtletterDTO.ListResponseDto> result;
}
