package com.edison.project.domain.space.service;

import com.edison.project.domain.space.dto.AiResponseDto;
import com.edison.project.domain.space.dto.SpaceMapRequestDto;
import com.edison.project.domain.space.dto.SpaceMapResponseDto;
import com.edison.project.domain.space.dto.SpaceSimilarityRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<AiResponseDto.AiVectorResponseDto> sendToAiServer(List<SpaceMapRequestDto.MapRequestDto> requestDtoList) {
        String aiServerUrl = "http://52.79.91.137:8000/ai";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<SpaceMapRequestDto.MapRequestDto>> entity = new HttpEntity<>(requestDtoList, headers);

        ResponseEntity<AiResponseDto.AiVectorResponseDto[]> response = restTemplate.postForEntity(
                aiServerUrl,
                entity,
                AiResponseDto.AiVectorResponseDto[].class
        );

        if (response.getBody() == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(response.getBody());
    }

    // ✅ 키워드 기반 유사도 상위 ID 요청용
    public AiResponseDto.AiSimilarityResponseDto sendToSimilarityServer(SpaceSimilarityRequestDto.MapRequestDto requestDto) {
        String similarityUrl = "http://52.79.91.137:8000/similarity";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SpaceSimilarityRequestDto.MapRequestDto> entity = new HttpEntity<>(requestDto, headers);

        ResponseEntity<AiResponseDto.AiSimilarityResponseDto> response = restTemplate.postForEntity(
                similarityUrl,
                entity,
                AiResponseDto.AiSimilarityResponseDto.class
        );

        return response.getBody();
    }

}
