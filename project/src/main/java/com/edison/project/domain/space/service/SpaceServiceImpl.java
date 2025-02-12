package com.edison.project.domain.space.service;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.space.dto.SpaceInfoResponseDto;
import com.edison.project.domain.space.dto.SpaceResponseDto;
import com.edison.project.domain.space.entity.Space;
import com.edison.project.domain.space.repository.SpaceRepository;
import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.repository.BubbleRepository;
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

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final SpaceRepository spaceRepository;
    private final BubbleRepository bubbleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MemberRepository memberRepository;

    public SpaceServiceImpl(SpaceRepository spaceRepository,
                            BubbleRepository bubbleRepository, MemberRepository memberRepository) {
        this.spaceRepository = spaceRepository;
        this.bubbleRepository = bubbleRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> processSpaces(CustomUserPrincipal userPrincipal, Pageable pageable) {
        Long memberId = userPrincipal.getMemberId();

        System.out.println("🔍 [Process Spaces] 실행 - 사용자 ID: " + memberId);

        // ✅ 기존 사용자의 Space 가져오기
        List<Space> spaces = spaceRepository.findByMemberId(memberId);
        System.out.println("📌 기존 사용자의 Space 개수: " + spaces.size());

        // ✅ 사용자의 삭제되지 않은 Bubble 페이징 처리
        Pageable unlimitedPageable = PageRequest.of(0, Integer.MAX_VALUE); // 최대 개수 가져오기
        Page<Bubble> bubblePage = bubbleRepository.findByMember_MemberIdAndIsTrashedFalse(memberId, unlimitedPageable);
        // 페이징 처리 삭제, 가져올 수 있는 최대 개수만큼 반환하는 코드 추가

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

        List<Space> newSpaces = parseGptResponse(gptResponse, bubbles, memberId);
        System.out.println("✅ 변환된 Space 개수: " + newSpaces.size());

        // ✅ 새로운 Space 업데이트
        for (Space space : newSpaces) {
            saveOrUpdateSpace(space);
        }


        // ✅ 기존 Space + 새로운 Space 반환
        spaces.addAll(newSpaces);

        List<SpaceResponseDto> spaceDtos = spaces.stream()
                .map(space -> new SpaceResponseDto(
                        space.getBubble(),    // ✅ Bubble 객체 전달
                        space.getContent(),
                        space.getX(),
                        space.getY(),
                        space.getGroup()
                ))
                .collect(Collectors.toList());


        return ApiResponse.onSuccess(SuccessStatus._OK, pageInfo, spaceDtos);
    }

    @Transactional
    public void saveOrUpdateSpace(Space newSpace) {
        List<Space> existingSpaces = spaceRepository.findByBubble_BubbleIdAndMemberId(
                newSpace.getBubble().getBubbleId(), newSpace.getMemberId()
        );

        Optional<Space> existingSpace = existingSpaces.stream().findFirst();

        if (existingSpace.isPresent()) {
            Space spaceToUpdate = existingSpace.get();
            spaceToUpdate.setX(newSpace.getX());
            spaceToUpdate.setY(newSpace.getY());
            spaceToUpdate.setContent(newSpace.getContent());
            spaceToUpdate.setGroup(newSpace.getGroup());
            spaceRepository.save(spaceToUpdate);
            System.out.println("🔄 기존 Space 업데이트 완료! ID: " + spaceToUpdate.getId());
        } else {
            spaceRepository.save(newSpace);
            spaceRepository.flush();
            System.out.println("🆕 새로운 Space 추가! ID: " + newSpace.getId());
        }
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

    private List<Space> parseGptResponse(String gptResponse, List<Bubble> bubbles, Long memberId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            System.out.println("🔍 Raw GPT Response (Before Parsing): " + gptResponse);

            // ✅ GPT 응답에서 JSON 추출
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

            // ✅ JSON 배열 파싱
            List<Map<String, Object>> parsedData = objectMapper.readValue(contentJson, new TypeReference<List<Map<String, Object>>>() {});
            System.out.println("✅ 변환된 Space 데이터: " + parsedData);

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
                int group = item.get("group") != null ? ((Number) item.get("group")).intValue() : 1;  // ✅ 기본 그룹 1

                spaces.add(new Space(content, x, y, group, bubble, memberId));
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
        promptBuilder.append("Ensure that ALL provided bubbles are assigned unique coordinates, distributed evenly across four quadrants centered at (0,0).\n");
        promptBuilder.append("Each item should have the following attributes:\n");
        promptBuilder.append("- id: A unique identifier for the item (integer).\n");
        promptBuilder.append("- content: A short keyword or phrase (1-2 words) representing the item's content.\n");
        promptBuilder.append("- x: A unique floating-point number for the x-coordinate (spread across four quadrants).\n");
        promptBuilder.append("- y: A unique floating-point number for the y-coordinate (spread across four quadrants).\n");
        promptBuilder.append("- group: An integer representing the item's group ID, starting from 1. If an item does not belong to any group, set this to `null`.\n\n");

        promptBuilder.append("### Rules:\n");
        promptBuilder.append("1. Each item must have a unique (x, y) coordinate, with a minimum spacing of 0.5.\n");
        promptBuilder.append("2. Items with similar topics should form visually distinct clusters.\n");
        promptBuilder.append("3. Clusters should be well-separated from each other but internally cohesive.\n");
        promptBuilder.append("4. **❗ Each group MUST contain between 5 and 8 items. This is MANDATORY. ❗**\n");
        promptBuilder.append("5. **If any group contains fewer than 5 or more than 8 items, YOU MUST re-cluster that group into smaller sub-groups, each containing 5-8 items.**\n");
        promptBuilder.append("6. **Continue re-clustering until ALL groups satisfy the 5-8 item rule. This process must be repeated as many times as necessary.**\n");
        promptBuilder.append("7. Groups must start with number 1 and increment sequentially (1, 2, 3, ...).\n");
        promptBuilder.append("8. The number of groups should be minimized, ideally around 1/4 of the total number of items.\n");
        promptBuilder.append("9. **Items do NOT have to belong to a group. However, if possible, items should be grouped based on topic similarity.**\n");
        promptBuilder.append("10. Extract the core meaning of each content item, reducing it to 1 or 2 essential words.\n");
        promptBuilder.append("11. The output MUST strictly include the `group` field for ALL items, even if it's `null`.\n");
        promptBuilder.append("12. The output MUST be valid JSON format as shown below, with NO explanations or extra text.\n\n");

        promptBuilder.append("### Response Format:\n");
        promptBuilder.append("[\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"id\": 1,\n");
        promptBuilder.append("    \"content\": \"Keyword\",\n");
        promptBuilder.append("    \"x\": 1.5,\n");
        promptBuilder.append("    \"y\": -0.5,\n");
        promptBuilder.append("    \"group\": 1\n");
        promptBuilder.append("  },\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"id\": 2,\n");
        promptBuilder.append("    \"content\": \"Topic\",\n");
        promptBuilder.append("    \"x\": -1.0,\n");
        promptBuilder.append("    \"y\": 0.8,\n");
        promptBuilder.append("    \"group\": null  // ✅ Example of an ungrouped item\n");
        promptBuilder.append("  }\n");
        promptBuilder.append("]\n\n");

        promptBuilder.append("⚠️ **DO NOT** include any explanations, comments, or extra formatting. Respond ONLY with the JSON array. ⚠️\n");

        Long lastKey = null;
        for (Long key : requestData.keySet()) {
            lastKey = key;
        }

        for (Map.Entry<Long, String> entry : requestData.entrySet()) {
            promptBuilder.append("- ID: ").append(entry.getKey()).append("\n");
            promptBuilder.append(entry.getValue()).append("\n");

            if (entry.getKey().equals(lastKey)) {
                promptBuilder.append("(This item MUST be placed at coordinates (0, 0))\n");
            }
        }

        return promptBuilder.toString();
    }

    public ResponseEntity<ApiResponse> getSpaceInfo() {
        List<Space> spaces = spaceRepository.findAll();
        System.out.println("Fetched Spaces: " + spaces.size());

        if (spaces.isEmpty()) {
            return ApiResponse.onFailure(ErrorStatus.NO_SPACES_FOUND);
        }

        Map<Integer, List<Space>> groupedSpaces = spaces.stream()
                .collect(Collectors.groupingBy(Space::getGroup));
        System.out.println("Grouped Spaces: " + groupedSpaces.keySet());

        int maxGroupId = groupedSpaces.keySet().stream().max(Integer::compareTo).orElse(0);
        System.out.println("Max Group ID: " + maxGroupId);

        List<SpaceInfoResponseDto> responseList = new ArrayList<>();

        for (int groupId = 1; groupId <= maxGroupId; groupId++) {
            List<Space> groupSpaces = groupedSpaces.getOrDefault(groupId, new ArrayList<>());
            System.out.println("Processing Group ID: " + groupId + ", Spaces: " + groupSpaces.size());

            if (groupSpaces.isEmpty()) {
                continue;
            }

            double minX = groupSpaces.stream().mapToDouble(Space::getX).min().orElse(0);
            double maxX = groupSpaces.stream().mapToDouble(Space::getX).max().orElse(0);
            double minY = groupSpaces.stream().mapToDouble(Space::getY).min().orElse(0);
            double maxY = groupSpaces.stream().mapToDouble(Space::getY).max().orElse(0);

            double centerX = (minX + maxX) / 2;
            double centerY = (minY + maxY) / 2;

            double radius = Math.sqrt(Math.pow(maxX - centerX, 2) + Math.pow(maxY - centerY, 2));

            responseList.add(new SpaceInfoResponseDto(groupId, centerX, centerY, radius));
        }

        System.out.println("Response List Size: " + responseList.size());
        return ApiResponse.onSuccess(SuccessStatus._OK, responseList);
    }

}


