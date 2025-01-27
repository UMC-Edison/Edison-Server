package com.edison.project.domain.space.service;

import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.repository.BubbleRepository;
import com.edison.project.domain.space.entity.Space;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpaceServiceImpl implements SpaceService {
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final BubbleRepository bubbleRepository;

    public SpaceServiceImpl(BubbleRepository bubbleRepository) {
        this.bubbleRepository = bubbleRepository;
    }

    @Override
    public List<Space> processSpaces() {
        // 1. Bubble 데이터를 가져옵니다.
        List<Bubble> bubbles = bubbleRepository.findAll();

        // 2. Bubble 데이터를 content로 변환
        List<Map<String, Object>> requestData = createRequestData(bubbles);

        // 3. requestData에서 content만 추출
        List<String> contents = requestData.stream()
                .map(data -> (String) data.get("content")) // "content" 필드만 추출
                .collect(Collectors.toList());

        // 4. GPT 호출
        String gptResponse = callGPTForGrouping(contents);

        // 5. GPT 응답 파싱 및 매핑
        return parseGptResponse(gptResponse, bubbles);
    }


    // Bubble 데이터를 content로 변환
    private List<Map<String, Object>> createRequestData(List<Bubble> bubbles) {
        List<Map<String, Object>> requestData = new ArrayList<>();

        for (Bubble bubble : bubbles) {
            // Bubble의 Labels 병합
            String labels = bubble.getLabels().stream()
                    .map(label -> label.getLabel().getName())
                    .collect(Collectors.joining(", "));

            // Bubble의 제목, 내용, 라벨을 병합
            String content = String.format(
                    "Title: %s\nContent: %s\nLabels: %s",
                    bubble.getTitle(),
                    bubble.getContent(),
                    labels.isEmpty() ? "None" : labels
            );

            // 요청 데이터 생성
            Map<String, Object> data = new HashMap<>();
            data.put("id", bubble.getBubbleId());  // 실제 Bubble ID
            data.put("content", content);

            requestData.add(data);
        }

        return requestData;
    }


    // GPT API 호출
    private String callGPTForGrouping(List<String> contents) {
        String openaiApiKey = System.getenv("openai_key");
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            throw new RuntimeException("OpenAI API 키가 환경변수에 설정되어 있지 않습니다.");
        }

        if (contents == null || contents.isEmpty()) {
            throw new IllegalArgumentException("요청 데이터가 비어 있습니다.");
        }

        // JSON 요청 본문 생성
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> message = Map.of("role", "system", "content", buildPrompt(contents));
        Map<String, Object> requestBody = Map.of("model", "gpt-3.5-turbo", "messages", List.of(message));
        String jsonBody;

        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("JSON 생성 중 오류 발생: " + e.getMessage(), e);
        }

        // API 요청
        OkHttpClient client = createHttpClientWithTimeout();
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + openaiApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                System.out.println("Response Code: " + response.code());
                System.out.println("Response Body: " + responseBody);
                throw new RuntimeException("OpenAI API 호출 실패: " + response.code() + " - " + responseBody);
            }

            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException("OpenAI API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // GPT 응답 파싱 및 Space 매핑
    private List<Space> parseGptResponse(String gptResponse, List<Bubble> bubbles) {
        try {
            String sanitizedResponse = sanitizeResponse(gptResponse);
            System.out.println("Sanitized GPT Response: " + sanitizedResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> parsedData = objectMapper.readValue(
                    sanitizedResponse, new TypeReference<List<Map<String, Object>>>() {});

            // 그룹화 결과를 저장할 리스트
            List<Space> spaces = new ArrayList<>();

            // 그룹화 로직: 같은 그룹끼리 묶음
            Map<Long, List<Map<String, Object>>> groupedById = parsedData.stream()
                    .collect(Collectors.groupingBy(item -> ((Number) item.get("id")).longValue()));

            for (Map.Entry<Long, List<Map<String, Object>>> entry : groupedById.entrySet()) {
                Long id = entry.getKey();
                List<Map<String, Object>> groupItems = entry.getValue();

                // ID에 맞는 Bubble 찾기
                Optional<Bubble> optionalBubble = bubbles.stream()
                        .filter(bubble -> bubble.getBubbleId().equals(id))
                        .findFirst();

                if (optionalBubble.isEmpty()) {
                    System.err.println("Warning: Bubble not found for ID: " + id);
                    continue; // 매칭되지 않는 ID는 스킵
                }

                Bubble bubble = optionalBubble.get();

                // 그룹 내용을 결합하여 하나의 Space로 생성
                StringBuilder contentBuilder = new StringBuilder();
                for (Map<String, Object> groupItem : groupItems) {
                    String content = (String) groupItem.get("content");
                    if (content != null && !content.isBlank()) {
                        contentBuilder.append(content).append("\n");
                    }
                }

                // 좌표 및 그룹 설정 (첫 번째 항목 기준)
                double x = ((Number) groupItems.get(0).get("x")).doubleValue();
                double y = ((Number) groupItems.get(0).get("y")).doubleValue();
                List<?> rawGroups = (List<?>) groupItems.get(0).get("groups");
                List<Integer> groups = rawGroups.stream()
                        .map(group -> group instanceof Number ? ((Number) group).intValue() : Integer.parseInt(group.toString()))
                        .collect(Collectors.toList());

                // Space 객체 생성
                Space space = new Space(
                        contentBuilder.toString().trim(), // 그룹화된 content
                        x,
                        y,
                        groups.stream().map(String::valueOf).toList(),
                        bubble.getBubbleId()
                );
                spaces.add(space);
            }

            // 디버그 출력
            System.out.println("=== 매핑 결과 ===");
            for (Space space : spaces) {
                System.out.println("ID: " + space.getId());
                System.out.println("Content: " + space.getContent());
                System.out.println("x: " + space.getX());
                System.out.println("y: " + space.getY());
                System.out.println("Groups: " + space.getGroups());
                System.out.println("----------------");
            }

            return spaces;

        } catch (Exception e) {
            System.err.println("GPT Response Parsing Error: " + e.getMessage());
            throw new RuntimeException("GPT 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(List<String> contents) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("You are tasked with categorizing content items and positioning them on a 2D grid.\n");
        promptBuilder.append("Each item should have the following attributes:\n");
        promptBuilder.append("- `id`: A unique identifier for the item (integer).\n");
        promptBuilder.append("- `content`: A string representing the item's content.\n");
        promptBuilder.append("- `x`: A unique floating-point number for the x-coordinate.\n");
        promptBuilder.append("- `y`: A unique floating-point number for the y-coordinate.\n");
        promptBuilder.append("- `groups`: A list of integers representing the item's group IDs.\n\n");
        promptBuilder.append("### Rules:\n");
        promptBuilder.append("1. Each item must have a unique `(x, y)` coordinate.\n");
        promptBuilder.append("2. Items with similar topics should have closer `(x, y)` coordinates.\n");
        promptBuilder.append("3. Items with different topics should have larger distances between their coordinates.\n");
        promptBuilder.append("4. Coordinates should include decimal values to express fine-grained similarity.\n");
        promptBuilder.append("5. Ensure `groups` contains only integers, and avoid any other data types.\n");
        promptBuilder.append("6. Return only valid JSON output in the following format:\n\n");
        promptBuilder.append("[\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"id\": <unique ID>,\n");
        promptBuilder.append("    \"content\": \"<Content here>\",\n");
        promptBuilder.append("    \"x\": <x-coordinate>,\n");
        promptBuilder.append("    \"y\": <y-coordinate>,\n");
        promptBuilder.append("    \"groups\": [<group IDs>]\n");
        promptBuilder.append("  }\n");
        promptBuilder.append("]\n\n");
        promptBuilder.append("Return only valid JSON output without any additional text or explanation.\n");

        promptBuilder.append("### Input Content:\n");
        for (String content : contents) {
            promptBuilder.append("- ").append(content).append("\n");
        }

        return promptBuilder.toString();
    }



    // GPT 요청 프롬프트 생성
    private String sanitizeResponse(String response) {
        try {
            if (response == null || response.isBlank()) {
                throw new RuntimeException("GPT 응답이 비어 있습니다.");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap;

            try {
                responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                throw new RuntimeException("GPT 응답이 JSON 형식이 아닙니다: " + response, e);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("'choices' 필드가 비어있습니다.");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || !message.containsKey("content")) {
                throw new RuntimeException("'message' 필드에 'content'가 없습니다.");
            }

            String content = (String) message.get("content");
            if (content == null || content.isBlank()) {
                throw new RuntimeException("'content' 값이 비어 있습니다.");
            }

            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            objectMapper.readTree(content);

            return content;

        } catch (Exception e) {
            System.err.println("GPT 응답 처리 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("응답 정리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private OkHttpClient createHttpClientWithTimeout() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃 설정
                .writeTimeout(30, TimeUnit.SECONDS)   // 쓰기 타임아웃 설정
                .readTimeout(60, TimeUnit.SECONDS)    // 읽기 타임아웃 설정
                .build();
    }

}

