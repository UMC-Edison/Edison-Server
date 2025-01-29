package com.edison.project.domain.space.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.domain.space.entity.MemberSpace;
import com.edison.project.domain.space.entity.Space;
import com.edison.project.domain.space.repository.MemberSpaceRepository;
import com.edison.project.domain.space.repository.SpaceRepository;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.repository.BubbleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.edison.project.global.security.CustomUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import okhttp3.*;
import java.util.*;

@Service
@Transactional
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
    @Transactional
    public ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, Pageable pageable) {
        Long memberId = userPrincipal.getMemberId();

        System.out.println("🔍 [Process Spaces] 실행 - 사용자 ID: " + memberId);

        // ✅ 기존 사용자의 Space 가져오기
        List<Space> spaces = memberSpaceRepository.findSpacesByMemberId(memberId);
        System.out.println("📌 기존 사용자의 Space 개수: " + spaces.size());

        // ✅ 사용자의 삭제되지 않은 Bubble 페이징 처리
        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsDeletedFalse(memberId, pageable);

        // ✅ Page 정보 설정
        PageInfo pageInfo = new PageInfo(
                bubblePage.getNumber(),
                bubblePage.getSize(),
                bubblePage.hasNext(),
                bubblePage.getTotalElements(),
                bubblePage.getTotalPages()
        );

        // ✅ Bubble 데이터 변환
        List<Bubble> bubbles = bubblePage.getContent();
        System.out.println("🫧 사용자의 Bubble 개수: " + bubbles.size());

        // ✅ 버블이 없을 경우 -> "작성된 버블이 없습니다." 메시지 반환
        if (bubbles.isEmpty()) {
            System.out.println("⚠️ 사용자에게 등록된 버블이 없습니다.");
            return ApiResponse.onFailure(ErrorStatus.NO_BUBBLES_FOUND);
        }

        Map<Long, String> requestData = createRequestDataWithId(bubbles);

        // ✅ GPT 호출하여 Space 좌표 변환
        String gptResponse = callGPTForGrouping(requestData);
        System.out.println("🛠 GPT 응답: " + gptResponse);

        List<Space> newSpaces = parseGptResponse(gptResponse, bubbles);
        System.out.println("✅ 변환된 Space 개수: " + newSpaces.size());

        // ✅ 새로운 Space를 저장하고 MemberSpace와 연결
        for (Space space : newSpaces) {
            saveSpaceAndMemberSpace(space, memberId);
        }

        // ✅ 기존 Space + 새로운 Space 반환
        spaces.addAll(newSpaces);

        List<SpaceResponseDto> spaceDtos = spaces.stream()
                .map(space -> new SpaceResponseDto(
                        space.getId(),
                        space.getContent(),
                        space.getX(),
                        space.getY(),
                        space.getGroupNames()
                ))
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, spaceDtos);
    }

    // ✅ Space와 MemberSpace 저장 및 즉시 반영
    @Transactional
    public void saveSpaceAndMemberSpace(Space space, Long memberId) {
        spaceRepository.save(space);
        spaceRepository.flush(); // ✅ 즉시 반영
        System.out.println("💾 저장된 Space ID: " + space.getId());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        MemberSpace memberSpace = new MemberSpace();
        memberSpace.setMember(member);
        memberSpace.setSpace(space);
        memberSpaceRepository.save(memberSpace);
        memberSpaceRepository.flush(); // ✅ 즉시 반영
        System.out.println("🔗 MemberSpace 연결됨: " + memberSpace.getMember().getMemberId() + " -> " + memberSpace.getSpace().getId());
    }

    // ✅ Bubble 데이터를 GPT 요청 형식으로 변환
    private Map<Long, String> createRequestDataWithId(List<Bubble> bubbles) {
        return bubbles.stream().collect(Collectors.toMap(
                Bubble::getBubbleId,
                bubble -> String.format("Title: %s\nContent: %s\nLabels: %s",
                        bubble.getTitle(),
                        bubble.getContent(),
                        bubble.getLabels().stream()
                                .map(label -> label.getLabel().getName())
                                .collect(Collectors.joining(", "))
                )
        ));
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
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(OPENAI_API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + openaiApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI API 호출 실패: " + response.code());
                }
                return response.body().string();
            }
        } catch (IOException e) {
            throw new RuntimeException("OpenAI API 호출 중 오류 발생: " + e.getMessage(), e);
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

    private List<Space> parseGptResponse(String gptResponse, List<Bubble> bubbles) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // ✅ 1. GPT 응답을 Map으로 변환
            Map<String, Object> responseMap = objectMapper.readValue(gptResponse, new TypeReference<Map<String, Object>>() {});

            // ✅ 2. "choices" 필드 확인
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("'choices' 필드가 비어 있음");
            }

            // ✅ 3. "message" 내부 "content" 확인
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || !message.containsKey("content")) {
                throw new RuntimeException("'message' 필드가 없거나 'content'가 없음");
            }

            // ✅ 4. "content" 값에서 JSON 문자열 추출 후 다시 변환
            String contentJson = (String) message.get("content");

            // ✅ JSON이 ```json ... ``` 형태일 경우 제거
            contentJson = contentJson.replaceAll("```json", "").replaceAll("```", "").trim();

            // ✅ 5. 문자열을 다시 Map으로 변환
            Map<String, Object> parsedContent = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {});

            // ✅ 6. "items" 필드 확인 및 리스트 변환
            List<Map<String, Object>> parsedData = (List<Map<String, Object>>) parsedContent.get("items");
            if (parsedData == null || parsedData.isEmpty()) {
                throw new RuntimeException("'items' 필드가 비어 있음");
            }

            System.out.println("✅ 변환된 Space 데이터: " + parsedData);

            // ✅ 7. Space 엔티티로 변환
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

                spaces.add(new Space(content, x, y, groups, bubble));
            }
            return spaces;

        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }



    // ✅ GPT 요청 프롬프트 생성
    private String buildPromptWithId(Map<Long, String> requestData) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are tasked with categorizing content items and positioning them on a 2D grid.\n");
        promptBuilder.append("Each item should have the following attributes:\n");
        promptBuilder.append("- id: A unique identifier for the item (integer).\n");
        promptBuilder.append("- content: A string representing the item's content.\n");
        promptBuilder.append("- x: A unique floating-point number for the x-coordinate.\n");
        promptBuilder.append("- y: A unique floating-point number for the y-coordinate.\n");
        promptBuilder.append("- groups: A list of integers representing the item's group IDs.\n\n");
        promptBuilder.append("### Rules:\n");
        promptBuilder.append("1. Each item must have a unique (x, y) coordinate.\n");
        promptBuilder.append("2. Items with similar topics should have closer (x, y) coordinates.\n");
        promptBuilder.append("3. Items with different topics should have larger distances between their coordinates.\n");
        promptBuilder.append("4. The spread or distance for groups is up to you to decide.\n");
        promptBuilder.append("5. Ensure groups contains only integers, and avoid any other data types.\n");
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


