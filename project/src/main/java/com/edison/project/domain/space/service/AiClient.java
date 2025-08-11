package com.edison.project.domain.space.service;

import com.edison.project.domain.space.dto.SpaceMapRequestDto;
import com.edison.project.domain.space.dto.SpaceSimilarityRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> sendToAiServer(SpaceMapRequestDto requestDto) {
        String aiServerUrl = "http://52.79.91.137:8000/ai";

        try {
            System.out.println("🔍 보내는 JSON: " + objectMapper.writeValueAsString(requestDto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SpaceMapRequestDto> entity = new HttpEntity<>(requestDto, headers);

        ResponseEntity<List> response = restTemplate.postForEntity(
                aiServerUrl,
                entity,
                List.class
        );

        return response.getBody();
    }

    // ✅ 키워드 기반 유사도 상위 ID 요청용
    public Map<String, Object> sendToSimilarityServer(SpaceSimilarityRequestDto.MapRequestDto requestDto) {
        String similarityUrl = "http://52.79.91.137:8000/similarity";

        try {
            System.out.println("키워드 유사도 요청 JSON: " + objectMapper.writeValueAsString(requestDto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SpaceSimilarityRequestDto.MapRequestDto> entity = new HttpEntity<>(requestDto, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                similarityUrl,
                entity,
                Map.class
        );

        return response.getBody();
    }

}
