package com.edison.project.domain.space.service;

import com.edison.project.domain.space.dto.SpaceMapRequestDto;
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

    public List<Map<String, Object>> sendToAiServer(SpaceMapRequestDto requestDto) {
        String aiServerUrl = "http://52.79.91.137:8000/ai";

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

}
