package com.edison.project.domain.space.service;

import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.repository.BubbleRepository;
import com.edison.project.domain.space.entity.Space;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.*;

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
        // 1. Bubble 테이블 전체 데이터 조회
        List<Bubble> bubbles = bubbleRepository.findAll();

        // 2. Bubble 데이터를 Space 객체로 변환
        List<Space> spaces = createSpacesFromBubbles(bubbles);

        // 3. GPT 호출 및 좌표/그룹 계산
        String gptResponse = callGPTForGrouping(spaces.stream().map(Space::getContent).toList());
        return parseGptResponse(gptResponse, spaces);
    }

    // Bubble 데이터를 Space로 변환
    private List<Space> createSpacesFromBubbles(List<Bubble> bubbles) {
        List<Space> spaces = new ArrayList<>();

        for (Bubble bubble : bubbles) {
            // Bubble과 연결된 Label 정보 가져오기
            List<String> labelNames = bubble.getLabels().stream()
                    .map(bubbleLabel -> bubbleLabel.getLabel().getName())
                    .toList();

            // Label 이름을 쉼표로 결합
            String labels = String.join(", ", labelNames);

            // Space content 생성
            String spaceContent = String.format(
                    "Title: %s\nContent: %s\nLabels: %s",
                    bubble.getTitle(),
                    bubble.getContent(),
                    labels.isEmpty() ? "None" : labels
            );

            spaces.add(new Space(spaceContent, 0., 0., new ArrayList<>(), bubble.getBubbleId()));
        }

        return spaces;
    }

    private OkHttpClient createHttpClientWithTimeout() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // 연결 타임아웃
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)   // 쓰기 타임아웃
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)    // 읽기 타임아웃
                .build();
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

    private List<Space> parseGptResponse(String gptResponse, List<Space> spaces) {
        try {
            String sanitizedResponse = sanitizeResponse(gptResponse);
            System.out.println("Sanitized GPT Response: " + sanitizedResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> parsedData = objectMapper.readValue(sanitizedResponse, new TypeReference<List<Map<String, Object>>>() {});

            int index = 0;
            for (Map<String, Object> item : parsedData) {
                if (!item.containsKey("content") || !item.containsKey("x") || !item.containsKey("y") || !item.containsKey("groups") || !item.containsKey("id")) {
                    System.out.println("Invalid item detected and skipped: " + item);
                    continue;
                }

                String content = (String) item.get("content");
                if (content == null || content.isBlank()) {
                    System.out.println("Skipping item with blank content: " + item);
                    continue;
                }

                double x = ((Number) item.get("x")).doubleValue();
                double y = ((Number) item.get("y")).doubleValue();
                Long id = ((Number) item.get("id")).longValue();

                List<?> rawGroups = (List<?>) item.get("groups");
                List<Integer> groups = new ArrayList<>();
                for (Object group : rawGroups) {
                    if (group instanceof Number) {
                        groups.add(((Number) group).intValue());
                    } else if (group instanceof String) {
                        try {
                            groups.add(Integer.parseInt((String) group));
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Invalid group format: " + group, e);
                        }
                    } else {
                        throw new RuntimeException("Invalid group format: " + group);
                    }
                }

                if (index < spaces.size()) {
                    Space space = spaces.get(index);
                    space.setContent(content);
                    space.setX(x);
                    space.setY(y);
                    space.setId(id);
                    space.setGroups(groups.stream().map(String::valueOf).toList());
                    System.out.println("Mapped Space: " + space.getContent());
                    index++;
                } else {
                    System.out.println("No more Space entities to map for item: " + item);
                }
            }

            System.out.println("=== 매핑 결과 ===");
            for (Space space : spaces) {
                System.out.println("ID: " + space.getId());
                System.out.println("Content: " + space.getContent());
                System.out.println("x: " + space.getX());
                System.out.println("y: " + space.getY());
                System.out.println("Groups: " + space.getGroups());
                System.out.println("----------------");
            }

        } catch (Exception e) {
            System.err.println("GPT Response Parsing Error: " + e.getMessage());
            throw new RuntimeException("GPT 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
        return spaces;
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

        promptBuilder.append("### Input Content:\n");
        for (String content : contents) {
            promptBuilder.append("- ").append(content).append("\n");
        }

        return promptBuilder.toString();
    }


}