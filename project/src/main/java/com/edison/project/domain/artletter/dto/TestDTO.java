package com.edison.project.domain.artletter.dto;

import com.edison.project.common.response.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TestDTO {
    private boolean isSuccess;
    private int code;
    private String message;
    private PageInfo pageInfo;
    private List<ArtletterDTO.ListResponseDto> result;
}
