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

            spaces.add(new Space(spaceContent, 0, 0, new ArrayList<>()));
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
    private String buildPrompt(List<String> contents) {
        StringBuilder promptBuilder = new StringBuilder();

        // 프롬프트 설명
        promptBuilder.append("You are tasked with categorizing and positioning content items on a 2D grid. ");
        promptBuilder.append("Each item should be assigned a unique `(x, y)` coordinate based on its category and relationships. ");
        promptBuilder.append("Return only valid JSON output, with no additional text, comments, or explanations.\n\n");

        // 규칙 추가
        promptBuilder.append("### Rules:\n");
        promptBuilder.append("1. Group similar items and assign `(x, y)` coordinates.\n");
        promptBuilder.append("2. Items in the same group should have closer coordinates.\n");
        promptBuilder.append("3. Items in different groups should be spaced farther apart.\n");
        promptBuilder.append("4. Ensure coordinates are unique and avoid clustering all items at `(0, 0)`.\n");
        promptBuilder.append("5. Return only a JSON array with objects in the following format:\n\n");

        // 출력 형식 명시
        promptBuilder.append("```json\n");
        promptBuilder.append("[\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("content: <Content here>");
        promptBuilder.append("x: <x-coordinate>");
        promptBuilder.append("y: <y-coordinate>");
        promptBuilder.append("groups: <groups, only in integer list>");

        // 콘텐츠 추가
        promptBuilder.append("### Input Content:\n");
        for (String content : contents) {
            promptBuilder.append("- ").append(content).append("\n");
        }

        return promptBuilder.toString();
    }


    private String sanitizeResponse(String response) {
        try {
            // 응답 문자열을 JSON 객체로 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});

            // "choices" -> 첫 번째 "message" -> "content" 필드 추출
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("'choices' 필드가 비어있습니다.");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || !message.containsKey("content")) {
                throw new RuntimeException("'message' 필드에 'content'가 없습니다.");
            }

            String content = (String) message.get("content");

            // 백틱(```) 제거 및 JSON 배열로 파싱 가능하도록 정리
            if (content.startsWith("```json")) {
                content = content.replace("```json", "").replace("```", "").trim();
            }

            // content가 유효한 JSON 배열인지 확인
            objectMapper.readTree(content); // JSON 파싱 시도 (유효성 확인용)
            return content;

        } catch (Exception e) {
            throw new RuntimeException("응답 정리 중 오류 발생: " + e.getMessage(), e);
        }
    }



    private List<Space> parseGptResponse(String gptResponse, List<Space> spaces) {
        try {
            // 1. 응답 정리 및 유효성 확인
            String sanitizedResponse = sanitizeResponse(gptResponse);
            System.out.println("Sanitized GPT Response: " + sanitizedResponse);

            // 2. JSON 배열로 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> parsedData = objectMapper.readValue(sanitizedResponse, new TypeReference<List<Map<String, Object>>>() {});

            // 3. 각 항목 매핑
            for (Map<String, Object> item : parsedData) {
                // 필수 필드 검증
                if (!item.containsKey("content") || !item.containsKey("x") || !item.containsKey("y") || !item.containsKey("groups")) {
                    System.out.println("Invalid item detected and skipped: " + item);
                    continue;
                }

                // 값 추출
                String content = (String) item.get("content");
                double x = ((Number) item.get("x")).doubleValue();
                double y = ((Number) item.get("y")).doubleValue();
                List<Integer> groups = (List<Integer>) item.get("groups");

                // Space 리스트에 값 매핑
                spaces.stream()
                        .filter(space -> space.getContent().equals(content))
                        .findFirst()
                        .ifPresent(space -> {
                            space.setX(x);
                            space.setY(y);
                            space.setGroups(groups.stream().map(String::valueOf).toList()); // Integer를 String으로 변환
                        });
            }

            // 4. 매핑 결과 확인 로그
            System.out.println("=== 매핑 결과 ===");
            for (Space space : spaces) {
                System.out.println("Content: " + space.getContent());
                System.out.println("x: " + space.getX());
                System.out.println("y: " + space.getY());
                System.out.println("Groups: " + space.getGroups());
                System.out.println("----------------");
            }
        } catch (Exception e) {
            System.out.println("GPT Response: " + gptResponse);
            throw new RuntimeException("GPT 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
        return spaces;
    }

}