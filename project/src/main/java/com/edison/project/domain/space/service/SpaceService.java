package com.edison.project.domain.space.service;

import java.util.List;

import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.global.security.CustomUserPrincipal;

public interface SpaceService {
    List<SpaceResponseDto> processSpaces(CustomUserPrincipal userPrincipal);
}
