package com.edison.project.domain.space.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.domain.space.entity.MemberSpace;
import com.edison.project.domain.space.entity.Space;
import com.edison.project.domain.space.repository.MemberSpaceRepository;
import com.edison.project.domain.space.repository.SpaceRepository;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.repository.BubbleRepository;

import com.edison.project.global.security.CustomUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SpaceServiceImpl implements SpaceService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final SpaceRepository spaceRepository;
    private final MemberSpaceRepository memberSpaceRepository;
    private final BubbleRepository bubbleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MemberRepository memberRepository;

    public SpaceServiceImpl(SpaceRepository spaceRepository,
                            MemberSpaceRepository memberSpaceRepository,
                            BubbleRepository bubbleRepository, MemberRepository memberRepository) {
        this.spaceRepository = spaceRepository;
        this.memberSpaceRepository = memberSpaceRepository;
        this.bubbleRepository = bubbleRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public List<SpaceResponseDto> processSpaces(CustomUserPrincipal userPrincipal) {
        Long memberId = userPrincipal.getMemberId();

        // ✅ 기존 사용자의 Space 가져오기
        List<Space> spaces = memberSpaceRepository.findSpacesByMemberId(memberId);

        // ✅ 새로운 Bubble 데이터를 가져와 GPT로 변환
        List<Bubble> bubbles = bubbleRepository.findAll();  // TODO: 사용자의 Bubble만 가져오도록 수정 가능
        Map<Long, String> requestData = createRequestDataWithId(bubbles);

        String gptResponse = callGPTForGrouping(requestData);
        List<Space> newSpaces = parseGptResponse(gptResponse, bubbles);

        // ✅ 새로운 Space를 저장하고 MemberSpace와 연결
        for (Space space : newSpaces) {
            spaceRepository.save(space);

            MemberSpace memberSpace = new MemberSpace();

            Member member = memberRepository.findById(userPrincipal.getMemberId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
            memberSpace.setMember(member);

            memberSpace.setSpace(space);
            memberSpaceRepository.save(memberSpace);
        }

        // ✅ 기존 Space + 새로운 Space 반환
        spaces.addAll(newSpaces);

        return spaces.stream()
                .map(space -> new SpaceResponseDto(
                        space.getId(),
                        space.getContent(),
                        space.getX(),
                        space.getY(),
                        space.getGroupNames()
                ))
                .collect(Collectors.toList());
    }

    // ✅ Bubble 데이터를 GPT 요청 형식으로 변환
    private Map<Long, String> createRequestDataWithId(List<Bubble> bubbles) {
        Map<Long, String> requestData = new HashMap<>();
        for (Bubble bubble : bubbles) {
            String labels = bubble.getLabels().stream()
                    .map(label -> label.getLabel().getName())
                    .collect(Collectors.joining(", "));
            String content = String.format("Title: %s\nContent: %s\nLabels: %s",
                    bubble.getTitle(), bubble.getContent(), labels.isEmpty() ? "None" : labels);
            requestData.put(bubble.getBubbleId(), content);
        }
        return requestData;
    }

    // ✅ GPT 호출하여 Space 좌표 변환
    private String callGPTForGrouping(Map<Long, String> requestData) {
        String openaiApiKey = System.getenv("openai_key");
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            throw new RuntimeException("OpenAI API 키가 환경변수에 설정되어 있지 않습니다.");
        }

        Map<String, Object> message = Map.of("role", "system", "content", buildPromptWithId(requestData));
        Map<String, Object> requestBody = Map.of("model", "gpt-3.5-turbo", "messages", List.of(message));

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
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
                    throw new RuntimeException("OpenAI API 호출 실패: " + response.code() + " - " + responseBody);
                }
                return response.body().string();
            }
        } catch (IOException e) {
            throw new RuntimeException("OpenAI API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private List<Space> parseGptResponse(String gptResponse, List<Bubble> bubbles) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // ✅ 1. GPT 응답을 Map으로 변환
            Map<String, Object> responseMap = objectMapper.readValue(gptResponse, new TypeReference<>() {});

            // ✅ 2. 'choices' 내부 메시지 추출
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("'choices' 필드가 비어 있음");
            }

            // ✅ 3. 'message' 내부 'content' 추출 (이 부분이 실제 JSON 데이터)
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || !message.containsKey("content")) {
                throw new RuntimeException("'message' 필드가 없거나 'content'가 없음");
            }

            // ✅ 4. 'content' 값(문자열 JSON)을 다시 ObjectMapper로 파싱
            String contentJson = (String) message.get("content");
            contentJson = contentJson.replaceAll("```json", "").replaceAll("```", "").trim();  // ✅ GPT가 코드 블록 감쌌을 경우 정리
            System.out.println("Sanitized Content JSON: " + contentJson);  // 디버깅용 출력

            // ✅ 5. 실제 Space 데이터를 List<Map>으로 변환
            List<Map<String, Object>> parsedData = objectMapper.readValue(
                    contentJson, new TypeReference<List<Map<String, Object>>>() {}
            );

            // ✅ 6. Space 엔티티로 변환
            List<Space> spaces = new ArrayList<>();
            for (Map<String, Object> item : parsedData) {
                Long id = ((Number) item.get("id")).longValue();
                Optional<Bubble> optionalBubble = bubbles.stream()
                        .filter(bubble -> bubble.getBubbleId().equals(id))
                        .findFirst();
                if (optionalBubble.isEmpty()) continue;

                Bubble bubble = optionalBubble.get();
                String content = (String) item.get("content");
                double x = ((Number) item.get("x")).doubleValue();
                double y = ((Number) item.get("y")).doubleValue();
                List<String> groups = ((List<?>) item.get("groups")).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());

                spaces.add(new Space(content, x, y, groups, bubble.getBubbleId()));
            }
            return spaces;

        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }


    // ✅ GPT 응답 데이터 정리
    private String sanitizeResponse(String response) {
        try {
            if (response == null || response.isBlank()) throw new RuntimeException("GPT 응답이 비어 있습니다.");
            objectMapper.readTree(response);
            return response.trim();
        } catch (IOException e) {
            throw new RuntimeException("GPT 응답 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // ✅ GPT 요청 프롬프트 생성
    private String buildPromptWithId(Map<Long, String> requestData) {
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
        promptBuilder.append("4. The spread or distance for groups is up to you to decide.\n");
        promptBuilder.append("5. Ensure `groups` contains only integers, and avoid any other data types.\n");
        promptBuilder.append("6. Return only valid JSON output in the following format:\n\n");

        for (Map.Entry<Long, String> entry : requestData.entrySet()) {
            promptBuilder.append("- ID: ").append(entry.getKey()).append("\n");
            promptBuilder.append(entry.getValue()).append("\n");
        }

        return promptBuilder.toString();
    }

    private OkHttpClient createHttpClientWithTimeout() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃 설정
                .writeTimeout(30, TimeUnit.SECONDS)   // 쓰기 타임아웃 설정
                .readTimeout(60, TimeUnit.SECONDS)    // 읽기 타임아웃 설정
                .build();
    }
}
