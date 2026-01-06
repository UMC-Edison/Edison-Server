package com.edison.project.domain.space.dto;

import com.edison.project.domain.space.entity.Space;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties ({"hibernateLazyInitializer", "handler"})
public class SpaceResponseDto {

    private final String localIdx;
    private final double x;
    private final double y;

    // Space 엔티티를 직접 받아 DTO를 생성

    public SpaceResponseDto(Space space) {
        this.localIdx = space.getBubble().getLocalIdx();
        this.x = space.getX();
        this.y = space.getY();
    }

}
