package com.edison.project.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GptConfig {

    @Value("${openai_key}")
    private String secretKey;

    private String model = "gpt-3.5-turbo";

    public String getSecretKey() {
        return secretKey;
    }

    public String getModel() {
        return model;
    }

    @Bean(name = "gptRestTemplate")
    public RestTemplate gptRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public HttpHeaders httpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
