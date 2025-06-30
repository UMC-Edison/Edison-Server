package com.edison.project.domain.space.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;
import com.edison.project.domain.bubble.entity.BubbleBacklink;
import com.edison.project.domain.bubble.entity.BubbleLabel;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.member.service.MemberService;
import com.edison.project.domain.space.dto.SpaceMapRequestDto;
import com.edison.project.domain.space.dto.SpaceMapResponseDto;
import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.domain.space.entity.Space;
import com.edison.project.domain.space.repository.SpaceRepository;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.repository.BubbleRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

@Service
@Transactional
public class SpaceServiceImpl implements SpaceService {

    private final MemberService memberService;

    @Value("${openai.secret-key}")
    private String secretKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final SpaceRepository spaceRepository;
    private final BubbleRepository bubbleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MemberRepository memberRepository;
    private final AiClient aiClient;

    public SpaceServiceImpl(SpaceRepository spaceRepository, BubbleRepository bubbleRepository,
                            MemberRepository memberRepository, MemberService memberService, AiClient aiClient) {
        this.spaceRepository = spaceRepository;
        this.bubbleRepository = bubbleRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.aiClient = aiClient;
    }

    private SpaceMapRequestDto.MapRequestDto convertToBubbleRequestDto(Bubble bubble) {
        return SpaceMapRequestDto.MapRequestDto.builder()
                .localIdx(bubble.getLocalIdx())
                .content(bubble.getContent())
                .build();
    };

    @Override
    @Transactional
    public List<SpaceMapResponseDto.MapResponseDto> mapBubbles(CustomUserPrincipal userPrincipal) {

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<Bubble> bubbles = bubbleRepository.findByMemberIdAndIsTrashedFalse(member.getMemberId());

        List<SpaceMapRequestDto.MapRequestDto> dtoList = bubbles.stream()
                .map(this::convertToBubbleRequestDto)
                .collect(Collectors.toList());

        SpaceMapRequestDto requestDto = SpaceMapRequestDto.builder()
                .memos(dtoList)
                .build();

        List<Map<String, Object>> aiResults = aiClient.sendToAiServer(requestDto);

        return aiResults.stream()
                .map(result -> SpaceMapResponseDto.MapResponseDto.builder()
                        .localIdx((String) result.get("id"))
                        .x(Double.parseDouble(result.get("x").toString()))
                        .y(Double.parseDouble(result.get("y").toString()))
                        .build())
                .collect(Collectors.toList());
    }

    /*
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, Pageable pageable, String userIdentityKeywords) {
        Long memberId = userPrincipal.getMemberId();
        System.out.println(" [Process Spaces - 전체] 실행 - 사용자 ID: " + memberId);

        Pageable unlimitedPageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsTrashedFalse(memberId, unlimitedPageable);
        List<Bubble> bubbles = bubblePage.getContent();
        System.out.println(" 사용자의 Bubble 개수: " + bubbles.size());

        if (bubbles.isEmpty()) {
            return ApiResponse.onFailure(ErrorStatus.NO_BUBBLES_FOUND);
        }

        Map<String, String> requestData = createRequestDataWithLocalIdx(bubbles);
        String gptResponse = callGPTForGrouping(requestData, userIdentityKeywords);
        List<Space> newSpaces = parseGptResponse(gptResponse, bubbles, memberId);

        spaceRepository.deleteByMemberId(memberId);
        spaceRepository.flush();
        spaceRepository.saveAll(newSpaces);

        List<SpaceResponseDto> spaceDtos = newSpaces.stream()
                .map(space -> new SpaceResponseDto(space.getBubble(), space.getX(), space.getY()))
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, spaceDtos);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, List<String> localIdxs, String userIdentityKeywords) {
        Long memberId = userPrincipal.getMemberId();
        System.out.println("[Process Spaces - 선택] 실행 - 사용자 ID: " + memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

        Set<Bubble> bubbleSet = bubbleRepository.findAllByMemberAndLocalIdxIn(member, new HashSet<>(localIdxs));
        List<Bubble> bubbles = new ArrayList<>(bubbleSet);
        System.out.println("선택된 Bubble 개수: " + bubbles.size());

        if (bubbles.isEmpty()) {
            return ApiResponse.onFailure(ErrorStatus.NO_BUBBLES_FOUND);
        }

        Map<String, String> requestData = createRequestDataWithLocalIdx(bubbles);
        String gptResponse = callGPTForGrouping(requestData, userIdentityKeywords);
        List<Space> newSpaces = parseGptResponse(gptResponse, bubbles, memberId);

        spaceRepository.deleteByMemberId(memberId);
        spaceRepository.flush();
        spaceRepository.saveAll(newSpaces);

        List<SpaceResponseDto> spaceDtos = newSpaces.stream()
                .map(space -> new SpaceResponseDto(space.getBubble(), space.getX(), space.getY()))
                .collect(Collectors.toList());

        return ApiResponse.onSuccess(SuccessStatus._OK, spaceDtos);
    }


    private Map<String, String> createRequestDataWithLocalIdx(List<Bubble> bubbles) {
        return bubbles.stream().collect(Collectors.toMap(
                Bubble::getLocalIdx,
                bubble -> String.format("Title: %s\nContent: %s\nLabels: %s",
                        bubble.getTitle(),
                        bubble.getContent(),
                        bubble.getLabels().stream()
                                .map(label -> label.getLabel().getName())
                                .collect(Collectors.joining(", "))
                )
        ));
    }


    private String callGPTForGrouping(Map<String, String> requestData, String userIdentityKeywords) {
        String openaiApiKey = secretKey;
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            throw new RuntimeException("OpenAI API 키가 환경변수에 설정되어 있지 않습니다.");
        }

        Map<String, Object> systemMessage = Map.of("role", "system", "content", "Forget everything before this prompt. Start fresh.");
        Map<String, Object> userMessage = Map.of("role", "user", "content", buildPromptWithLocalIdx(requestData, userIdentityKeywords));
        Map<String, Object> requestBody = Map.of("model", "gpt-3.5-turbo", "messages", List.of(systemMessage, userMessage));

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

    private List<Space> parseGptResponse(String gptResponse, List<Bubble> bubbles, Long memberId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            System.out.println("🔍 Raw GPT Response (Before Parsing): " + gptResponse);

            // GPT 응답에서 JSON 추출
            Map<String, Object> responseMap = objectMapper.readValue(gptResponse, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("'choices' 필드가 비어 있음");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null || !message.containsKey("content")) {
                throw new RuntimeException("'message' 필드가 없거나 'content'가 없음");
            }

            String contentJson = (String) message.get("content");
            contentJson = contentJson.replaceAll("```json", "").replaceAll("```", "").trim();

            // JSON 배열 파싱
            List<Map<String, Object>> parsedData = objectMapper.readValue(contentJson, new TypeReference<List<Map<String, Object>>>() {});
            System.out.println("변환된 Space 데이터: " + parsedData);

            List<Space> spaces = new ArrayList<>();
            for (Map<String, Object> item : parsedData) {
                String localIdx = (String) item.get("id");

                Optional<Bubble> optionalBubble = bubbles.stream()
                        .filter(bubble -> localIdx.equals(bubble.getLocalIdx()))
                        .findFirst();


                if (optionalBubble.isEmpty()) continue;

                Bubble bubble = optionalBubble.get();
                String content = (String) item.get("content");
                double x = ((Number) item.get("x")).doubleValue();
                double y = ((Number) item.get("y")).doubleValue();

                spaces.add(new Space(content, x, y, bubble, memberId));
            }
            return spaces;

        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // GPT 요청 프롬프트 생성
    private String buildPromptWithLocalIdx(Map<String, String> requestData, String userIdentityKeywords) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Forget everything before this prompt. Start fresh with no prior memory.\n");

        promptBuilder.append("You are tasked with categorizing content items and positioning them on a 2D grid.\n");
        promptBuilder.append("Ensure that ALL provided bubbles are assigned unique coordinates, distributed evenly across four quadrants centered at (0,0).\n");
        promptBuilder.append("Each item should have the following attributes:\n");
        promptBuilder.append("- id: A unique identifier for the item (String).\n");
        promptBuilder.append("- content: A short keyword or phrase (1-2 words) representing the item's content.\n");
        promptBuilder.append("- x: A unique floating-point number for the x-coordinate (spread across four quadrants).\n");
        promptBuilder.append("- y: A unique floating-point number for the y-coordinate (spread across four quadrants).\n");

        promptBuilder.append("### Rules:\n");
        promptBuilder.append("1. Each item must have a unique (x, y) coordinate, with a minimum Euclidean distance of 0.5 between any two items.\n");
        promptBuilder.append("2. Items with similar topics should form visually distinct clusters.\n");
        promptBuilder.append("3. Clusters should be well-separated from each other but internally cohesive.\n");
        promptBuilder.append("4. **❗ Each group MUST contain between 5 and 8 items. This is MANDATORY. ❗**\n");
        promptBuilder.append("5. **If any group contains fewer than 5 or more than 8 items, YOU MUST re-cluster that group into smaller sub-groups, each containing 5-8 items.**\n");
        promptBuilder.append("6. **Continue re-clustering until ALL groups satisfy the 5-8 item rule. This process must be repeated as many times as necessary.**\n");
        promptBuilder.append("7. The number of groups should be minimized, ideally around 1/4 of the total number of items.\n");
        promptBuilder.append("8. **Items do NOT have to belong to a group. However, if possible, items should be grouped based on topic similarity.**\n");
        promptBuilder.append("9. Extract the core meaning of each content item, reducing it to 1 or 2 essential words.\n");
        promptBuilder.append("10. The output MUST strictly include the `group` field for ALL items, even if it's `null`.\n");
        promptBuilder.append("11. The output MUST be valid JSON format as shown below, with NO explanations or extra text.\n\n");

        promptBuilder.append("### Response Format:\n");
        promptBuilder.append("[\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"id\":  \"new-abc-123\",\n");
        promptBuilder.append("    \"content\": \"Keyword\",\n");
        promptBuilder.append("    \"x\": 1.5,\n");
        promptBuilder.append("    \"y\": -0.5,\n");
        promptBuilder.append("  },\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"id\": 2,\n");
        promptBuilder.append("    \"content\": \"Topic\",\n");
        promptBuilder.append("    \"x\": -1.0,\n");
        promptBuilder.append("    \"y\": 0.8,\n");
        promptBuilder.append("  }\n");
        promptBuilder.append("]\n\n");

        promptBuilder.append("\n### Important Rules:\n");
        promptBuilder.append("1. **Each item MUST have a unique (x, y) coordinate. Do NOT use (0,0) for all items.**\n");
        promptBuilder.append("2. **Ensure that NO two items have the same exact coordinates.**\n");
        promptBuilder.append("⚠️ **DO NOT** include any explanations, comments, or extra formatting. Respond ONLY with the JSON array. ⚠️\n");

        // 사용자 프로필 정보 추가
        promptBuilder.append("\n### 사용자 프로필 정보:\n");
        promptBuilder.append("The user has the following identity characteristics that should influence content positioning:\n");

        // 카테고리별 설명 추가
        promptBuilder.append("- **CATEGORY1 (Words that describe the user)**: These keywords define the user's personality, traits, and strengths.\n");
        promptBuilder.append("- **CATEGORY2 (User's future field)**: These keywords represent the user's aspirations, career, or areas of interest for the future.\n");
        promptBuilder.append("- **CATEGORY3 (Most inspiring environment for the user)**: These words describe places, situations, or conditions where the user feels most inspired and productive.\n");
        promptBuilder.append("- **CATEGORY4 (Fields that stimulate the user's imagination)**: These keywords show which fields, subjects, or themes spark the user's creativity.\n");

        promptBuilder.append("\nThe user's identity profile based on these categories:\n");
        promptBuilder.append(userIdentityKeywords).append("\n");

        promptBuilder.append("Consider this information when determining relationships and positions between items.\n\n");


        String lastKey = null;
        for (String key : requestData.keySet()) {
            lastKey = key;
        }

        for (Map.Entry<String, String> entry : requestData.entrySet()) {
            promptBuilder.append("- ID: ").append(entry.getKey()).append("\n");
            promptBuilder.append(entry.getValue()).append("\n");

            if (entry.getKey().equals(lastKey)) {
                promptBuilder.append("(This item MUST be placed at coordinates (0, 0))\n");
            }
        }

        return promptBuilder.toString();
    }
    */
}


