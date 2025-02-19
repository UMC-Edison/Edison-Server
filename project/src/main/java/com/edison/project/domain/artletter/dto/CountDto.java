package com.edison.project.domain.artletter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CountDto {
    private Long artletterId;
    private Long count;
}
