package com.edison.project.domain.space.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WordResultDto {
    private String word;
    private String pos;
    private String definition;
    private String example;
}
