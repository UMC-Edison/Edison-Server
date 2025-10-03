package com.edison.project.domain.member.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.keywords.entity.Keywords;
import com.edison.project.domain.keywords.repository.KeywordsRepository;
import com.edison.project.domain.member.dto.MemberRequestDto;
import com.edison.project.domain.member.dto.MemberResponseDto;
import com.edison.project.domain.member.entity.MemberKeyword;
import com.edison.project.domain.member.entity.RefreshToken;
import com.edison.project.domain.member.repository.MemberKeywordRepository;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.member.repository.RefreshTokenRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import com.edison.project.global.util.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edison.project.domain.member.entity.Member;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.edison.project.common.status.SuccessStatus._OK;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final MemberKeywordRepository memberKeywordRepository;
    private final KeywordsRepository keywordsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    // idToken으로 회원가입/로그인 API
    @Override
    @Transactional
    public MemberResponseDto.SignupResultDto processGoogleSignup(String idToken, MemberRequestDto.IdentityTestSaveDto request) {

        // Google idToken에서 사용자 정보 추출
        GoogleIdToken.Payload payload = jwtUtil.verifyGoogleIdToken(idToken);
        String email = payload.getEmail();

        if (memberRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorStatus.ALREADY_REGISTERED);
        }

        Member member = Member.builder()
                .email(email)
                .build();
        memberRepository.save(member);

        Long memberId = memberRepository.findMemberIdByEmail(email);

        //토큰발급
        MemberResponseDto.TokenDto tokens = generateTokens(memberId, email);

        //아이덴티티 설정
        MemberResponseDto.IdentityTestSaveResultDto identityDto = saveIdentityTest(memberId, request);

        return MemberResponseDto.SignupResultDto.builder()
                .memberId(memberId)
                .email(email)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .identity(identityDto)
                .build();
    }

    @Override
    @Transactional
    public MemberResponseDto.LoginResultDto processGoogleLogin(String idToken) {

        // Google idToken에서 사용자 정보 추출
        GoogleIdToken.Payload payload = jwtUtil.verifyGoogleIdToken(idToken);
        String email = payload.getEmail();

        Long memberId = memberRepository.findOptionalMemberIdByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        MemberResponseDto.TokenDto tokens = generateTokens(memberId, email);

        return MemberResponseDto.LoginResultDto.builder()
                .memberId(memberId)
                .email(email)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    @Transactional
    @Override
    public MemberResponseDto.TokenDto generateTokens(Long memberId, String email) {
        String accessToken = jwtUtil.generateAccessToken(memberId, email);
        String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByEmail(email);

        if (existingToken.isPresent()) {
            // 기존 토큰이 있으면 업데이트
            RefreshToken tokenEntity = existingToken.get();
            tokenEntity.updateToken(refreshToken);
            refreshTokenRepository.save(tokenEntity);
        } else {
            // 기존 토큰이 없으면 새로 저장
            RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
            refreshTokenRepository.save(tokenEntity);
        }

        return MemberResponseDto.TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 토큰 생성 API(백엔드 전용)
    @Override
    @Transactional
    public MemberResponseDto.LoginResultDto generateTokensForOidcUser(String email) {

        AtomicReference<Boolean> isNew = new AtomicReference<>(false);
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    isNew.set(true);
                    Member newMember = Member.builder()
                            .email(email)
                            .role("ROLE_USER")
                            .build();
                    return memberRepository.save(newMember);
                });

        String accessToken = jwtUtil.generateAccessToken(member.getMemberId(), member.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(member.getMemberId(), member.getEmail());

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByEmail(email);

        if (existingToken.isPresent()) {
            // 기존 토큰이 있으면 업데이트
            RefreshToken tokenEntity = existingToken.get();
            tokenEntity.updateToken(refreshToken);
            refreshTokenRepository.save(tokenEntity);
        } else {
            // 기존 토큰이 없으면 새로 저장
            RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
            refreshTokenRepository.save(tokenEntity);
        }

        return MemberResponseDto.LoginResultDto.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 로그아웃 API
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> logout(CustomUserPrincipal userPrincipal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) authentication.getCredentials();

        long ttl = jwtUtil.getRemainingTime(token);

        // access토큰 블랙리스트에 추가
        redisTokenService.addToBlacklist(token, ttl);
        // refresh 토큰 삭제
        refreshTokenRepository.deleteByEmail(userPrincipal.getEmail());

        return ApiResponse.onSuccess(_OK);
    }

    // 액세스토큰 재발급 API
    @Override
    public ResponseEntity<ApiResponse> refreshAccessToken(String refreshToken) {

        Long memberId = jwtUtil.extractUserId(refreshToken);
        String email = jwtUtil.extractEmail(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(memberId, email);

        MemberResponseDto.RefreshResultDto response = MemberResponseDto.RefreshResultDto.builder()
                .accessToken(newAccessToken)
                .build();

        return ApiResponse.onSuccess(SuccessStatus._OK, response);

    }

    // 회원 탈퇴 API
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> cancel(CustomUserPrincipal userPrincipal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // access토큰 블랙리스트에 추가
        String token = (String) authentication.getCredentials();
        redisTokenService.addToBlacklist(token,jwtUtil.getRemainingTime(token));

        // refresh토큰 삭제
        refreshTokenRepository.deleteByEmail(userPrincipal.getEmail());

        //member 삭제
        memberRepository.deleteByMemberId(userPrincipal.getMemberId());

        return ApiResponse.onSuccess(_OK);
    }


    // 개인정보 설정 API
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.CreateProfileDto request) {

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());
        validateNickname(request.getNickname());

        if (member.getNickname() != null) {
            throw new GeneralException(ErrorStatus.NICKNAME_ALREADY_SET);
        }

        member.setNickname(request.getNickname());
        memberRepository.save(member);

        return ApiResponse.onSuccess(SuccessStatus._OK, new MemberResponseDto.CreateProfileResultDto(member.getNickname()));
    }


    // 개인정보 변경 API
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.UpdateProfileDto request) {

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());
        validateNickname(request.getNickname());

        if(Objects.equals(member.getNickname(), request.getNickname()) && Objects.equals(member.getProfileImg(), request.getImageUrl())){
            throw new GeneralException(ErrorStatus.PROFILE_NOT_CHANGED);
        }

        updateNicknameOrProfile(member, request.getNickname(), request.getImageUrl());
        memberRepository.save(member);

        return ApiResponse.onSuccess(SuccessStatus._OK, new MemberResponseDto.UpdateProfileResultDto(member.getNickname(), member.getProfileImg()));

    }


    // 개인정보 변경 API - 이미지 여부에 따른 빌드 로직 분리
    private void updateNicknameOrProfile(Member member, String nickname, String imageUrl) {
        if (imageUrl == null) {
            member.setNickname(nickname);
        } else {
            member.setNickname(nickname);
            member.setProfileImg(imageUrl);
        }
    }



    // 개인정보 조회 API
    @Override
    public ResponseEntity<ApiResponse> getProfile(CustomUserPrincipal userPrincipal) {

        Member member = memberRepository.findByMemberId(userPrincipal.getMemberId());
        return ApiResponse.onSuccess(SuccessStatus._OK,
                new MemberResponseDto.ProfileResultDto(
                        member.getEmail(),
                        member.getNickname(),
                        member.getProfileImg()
                )
        );
    }

    @Transactional
    public MemberResponseDto.IdentityTestSaveResultDto saveIdentityTest(Long memberId, MemberRequestDto.IdentityTestSaveDto request) {

        // 존재하지 않는 카테고리 검증
        List<String> validCategories = keywordsRepository.findDistinctCategories();
        String category = request.getCategory();
        if (!validCategories.contains(category)) {
            throw new GeneralException(ErrorStatus.INVALID_CATEGORY);
        }

        // 카테고리별로 최초 설정 여부 검증
        boolean isCategorySet = memberKeywordRepository.existsByMember_MemberIdAndKeyword_Category(memberId, category);
        if (isCategorySet) {
            throw new GeneralException(ErrorStatus.IDENTITY_ALREADY_SET);
        }

        // 카테고리-키워드 맵핑 검증
        List<Keywords> keywords = keywordsRepository.findAllById(request.getKeywords());
        if (!keywords.stream().allMatch(keyword -> category.equals(keyword.getCategory()))) {
            throw new GeneralException(ErrorStatus.INVALID_IDENTITY_MAPPING);
        }

        Member member = memberRepository.findByMemberId(memberId);
        // 새로운 키워드 저장
        List<MemberKeyword> memberKeywords = keywords.stream()
                .map(keyword -> MemberKeyword.builder()
                        .member(member)
                        .keyword(keyword)
                        .build())
                .collect(Collectors.toList());
        memberKeywordRepository.saveAll(memberKeywords);

        return MemberResponseDto.IdentityTestSaveResultDto.builder()
                .category(category)
                .keywords(request.getKeywords())
                .build();
    }

    @Override
    public MemberResponseDto.IdentityKeywordsResultDto getIdentityKeywords(CustomUserPrincipal userPrincipal) {

        // 키워드 테이블에서, 저장된 순서대로 모든 카테고리를 가져옴
        List<Keywords> keywords = keywordsRepository.findAllByOrderByCategoryAsc();

        // 사용자가 설정한 키워드만 필터링 -> 카테고리별로 그룹화
        Map<String, List<MemberResponseDto.IdentityKeywordDto>> categoryKeywords = keywords.stream()
                .filter(keyword -> memberKeywordRepository.existsByMember_MemberIdAndKeyword_KeywordId(userPrincipal.getMemberId(), keyword.getKeywordId()))
                .collect(Collectors.groupingBy(
                        Keywords::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                keyword -> MemberResponseDto.IdentityKeywordDto.builder()
                                        .keywordId(keyword.getKeywordId())
                                        .keywordName(keyword.getName())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        return MemberResponseDto.IdentityKeywordsResultDto.builder()
                .categories(categoryKeywords)
                .build();
    }

    @Transactional
    @Override
    public MemberResponseDto.IdentityTestSaveResultDto updateIdentityTest(CustomUserPrincipal userPrincipal, MemberRequestDto.IdentityTestSaveDto request) {

        Long memberId = userPrincipal.getMemberId();

        // 존재하지 않는 카테고리 검증
        String category = request.getCategory();
        List<String> validCategories = List.of("CATEGORY1", "CATEGORY2", "CATEGORY3", "CATEGORY4");
        if (!validCategories.contains(category)) {
            throw new GeneralException(ErrorStatus.INVALID_CATEGORY);
        }

        // 카테고리-키워드 맵핑 검증
        List<Keywords> keywords = keywordsRepository.findAllById(request.getKeywords());
        if (!keywords.stream().allMatch(keyword -> category.equals(keyword.getCategory()))) {
            throw new GeneralException(ErrorStatus.INVALID_IDENTITY_MAPPING);
        }

        // 요청된 키워드 ID를 가져옵니다.
        List<Integer> requestedKeywordIds = request.getKeywords();

        // 데이터베이스에서 해당 카테고리에 속하는 모든 키워드 ID를 조회합니다.
        List<Integer> validKeywordIds = keywordsRepository.findAllByCategory(request.getCategory()).stream()
                .map(Keywords::getKeywordId)
                .collect(Collectors.toList());

        // 요청된 키워드 ID 중에서 존재하지 않는 키워드 ID를 필터링합니다.
        List<Integer> invalidKeywordIds = requestedKeywordIds.stream()
                .filter(keywordId -> !validKeywordIds.contains(keywordId))
                .collect(Collectors.toList());

        // 존재하지 않는 키워드가 있다면 에러를 throw 합니다.
        if (!invalidKeywordIds.isEmpty()) {
            throw new GeneralException(ErrorStatus.NOT_EXISTS_KEYWORD);
        }

        List<MemberKeyword> existingMemberKeywords = memberKeywordRepository.findByMember_MemberIdAndKeywordCategory(memberId, request.getCategory());
        List<Integer> existingKeywordIds = existingMemberKeywords.stream()
                .map(memberKeyword -> memberKeyword.getKeyword().getKeywordId())
                .collect(Collectors.toList());

        // 이전 키워드와 비교하여 동일한지 확인
        if (new HashSet<>(existingKeywordIds).equals(new HashSet<>(request.getKeywords()))) {
            throw new GeneralException(ErrorStatus.NO_CHANGES_IN_KEYWORDS);
        }

        memberKeywordRepository.deleteByMember_MemberIdAndKeywordCategory(memberId, category);


        Member member = memberRepository.findByMemberId(memberId);
        List<MemberKeyword> memberKeywords = keywords.stream()
                .map(keyword -> MemberKeyword.builder()
                        .member(member)
                        .keyword(keyword)
                        .build())
                .collect(Collectors.toList());
        memberKeywordRepository.saveAll(memberKeywords);

        return MemberResponseDto.IdentityTestSaveResultDto.builder()
                .category(category)
                .keywords(request.getKeywords())
                .build();
    }

    /*
    공통 메서드 모음
     */

    // 닉네임 검증 로직
    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_NICKNAME);
        }
        if (nickname.length() > 20) {
            throw new GeneralException(ErrorStatus.NICKNAME_TOO_LONG);
        }
    }

    // 아이덴티티 키워드를 카테고리별로 가져와 스트링으로 반환
    @Override
    public String getCategorizedIdentityKeywords(CustomUserPrincipal userPrincipal) {

        Long memberId = userPrincipal.getMemberId();

        // 멤버의 카테고리별 키워드 가져오기
        List<MemberKeyword> memberKeywords = memberKeywordRepository.findByMember_MemberId(memberId);
        Map<String, List<String>> categorizedKeywords = memberKeywords.stream()
                .collect(Collectors.groupingBy(
                        mk -> mk.getKeyword().getCategory(),
                        Collectors.mapping(mk -> mk.getKeyword().getName(), Collectors.toList())
                ));

        // 각 카테고리에 대한 값을 가져오고, 없으면 빈 문자열 처리
        String category1 = String.join(", ", categorizedKeywords.getOrDefault("CATEGORY1", new ArrayList<>()));
        String category2 = String.join(", ", categorizedKeywords.getOrDefault("CATEGORY2", new ArrayList<>()));
        String category3 = String.join(", ", categorizedKeywords.getOrDefault("CATEGORY3", new ArrayList<>()));
        String category4 = String.join(", ", categorizedKeywords.getOrDefault("CATEGORY4", new ArrayList<>()));

        // 최종 문자열 포맷팅
        return String.format("category1: %s / category2: %s / category3: %s / category4: %s",
                category1, category2, category3, category4);
    }

}
