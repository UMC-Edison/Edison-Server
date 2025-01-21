package com.edison.project.domain.space.service;

import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.repository.BubbleRepository;
import com.edison.project.domain.space.entity.Space;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;

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
        promptBuilder.append("Given the following spaces, group them into categories and assign (x, y) coordinates. ");
        promptBuilder.append("If a space belongs to multiple groups, place it at the average of the clusters. ");
        promptBuilder.append("Return the result in JSON format:\n");

        for (String content : contents) {
            promptBuilder.append("- ").append(content).append("\n");
        }

        return promptBuilder.toString();
    }


    // GPT 응답 파싱 및 Space 객체 생성
    private List<Space> parseGptResponse(String gptResponse, List<Space> spaces) {
        System.out.println("GPT Response: " + gptResponse);
        try {
            // 1. OpenAI 응답 JSON 파싱
            Map<String, Object> responseMap = objectMapper.readValue(gptResponse, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("OpenAI API 응답에서 'choices'가 비어 있습니다.");
            }

            // 2. 첫 번째 choice의 message에서 content 가져오기
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || !message.containsKey("content")) {
                throw new RuntimeException("OpenAI API 응답에서 'message'가 비어 있거나 'content'가 없습니다.");
            }

            String content = (String) message.get("content");

            // 3. content를 다시 JSON으로 파싱
            List<Map<String, Object>> groups = objectMapper.readValue(content, List.class);

            // 4. Space 객체 업데이트
            for (Map<String, Object> group : groups) {
                String groupTitle = (String) group.get("group_title");
                List<Map<String, Object>> groupSpaces = (List<Map<String, Object>>) group.get("spaces");

                for (Map<String, Object> groupSpace : groupSpaces) {
                    String spaceContent = (String) groupSpace.get("content");
                    double x = (double) groupSpace.get("x");
                    double y = (double) groupSpace.get("y");

                    spaces.stream()
                            .filter(space -> space.getContent().equals(spaceContent))
                            .findFirst()
                            .ifPresent(space -> {
                                space.setX(x);
                                space.setY(y);
                                space.setGroups(Collections.singletonList(groupTitle));
                            });
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
        return spaces;
    }

}
