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
    public ResponseEntity<ApiResponse> processGoogleLogin(String idToken) {

        // Google idToken에서 사용자 정보 추출
        GoogleIdToken.Payload payload = jwtUtil.verifyGoogleIdToken(idToken);
        String email = payload.getEmail();

        MemberResponseDto.LoginResultDto dto = generateTokensForOidcUser(email);

        return ApiResponse.onSuccess(SuccessStatus._OK, dto);
    }

    // 토큰 생성 API
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
                .isNewMember(isNew)
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

        Member member = validateMember(userPrincipal);
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

        Member member = validateMember(userPrincipal);
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

        Member member = validateMember(userPrincipal);

        return ApiResponse.onSuccess(SuccessStatus._OK,
                new MemberResponseDto.ProfileResultDto(
                        member.getEmail(),
                        member.getNickname(),
                        member.getProfileImg()
                )
        );
    }


    @Override
    @Transactional
    public MemberResponseDto.IdentityTestSaveResultDto saveIdentityTest(CustomUserPrincipal userPrincipal, MemberRequestDto.IdentityTestSaveDto request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // 사용자 인증 확인
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 존재하지 않는 카테고리 검증
        List<String> validCategories = keywordsRepository.findDistinctCategories();
        String category = request.getCategory();
        if (!validCategories.contains(category)) {
            throw new GeneralException(ErrorStatus.INVALID_CATEGORY);
        }

        // 카테고리별로 최초 설정 여부 검증
        boolean isCategorySet = memberKeywordRepository.existsByMember_MemberIdAndKeyword_Category(member.getMemberId(), category);
        if (isCategorySet) {
            throw new GeneralException(ErrorStatus.IDENTITY_ALREADY_SET);
        }

        // 카테고리-키워드 맵핑 검증
        List<Keywords> keywords = keywordsRepository.findAllById(request.getKeywords());
        if (!keywords.stream().allMatch(keyword -> category.equals(keyword.getCategory()))) {
            throw new GeneralException(ErrorStatus.INVALID_IDENTITY_MAPPING);
        }

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
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Long memberId = userPrincipal.getMemberId();

        // 키워드 테이블에서, 저장된 순서대로 모든 카테고리를 가져옴
        List<Keywords> keywords = keywordsRepository.findAllByOrderByCategoryAsc();

        // 사용자가 설정한 키워드만 필터링 -> 카테고리별로 그룹화
        Map<String, List<MemberResponseDto.IdentityKeywordDto>> categoryKeywords = keywords.stream()
                .filter(keyword -> memberKeywordRepository.existsByMember_MemberIdAndKeyword_KeywordId(memberId, keyword.getKeywordId()))
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
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        // 사용자 인증 확인
        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // 존재하지 않는 카테고리 검증
        String category = request.getCategory();
        List<String> validCategories = List.of("CATEGORY1", "CATEGORY2", "CATEGORY3", "CATEGORY4");
        if (!validCategories.contains(category)) {
            throw new GeneralException(ErrorStatus.INVALID_CATEGORY);
        }

        // 요청된 키워드 조회 및 검증 (한 번만 조회)
        List<Keywords> keywords = keywordsRepository.findAllById(request.getKeywords());

        // 존재하지 않는 키워드가 있는지 확인
        if (keywords.size() != request.getKeywords().size()) {
            throw new GeneralException(ErrorStatus.NOT_EXISTS_KEYWORD);
        }

        // 요청된 키워드가 모두 해당 카테고리에 속하는지 확인
        Set<String> keywordCategories = keywords.stream().map(Keywords::getCategory).collect(Collectors.toSet());
        if (keywordCategories.size() != 1 || !keywordCategories.contains(category)) {
            throw new GeneralException(ErrorStatus.INVALID_IDENTITY_MAPPING);
        }


        List<MemberKeyword> existingMemberKeywords = memberKeywordRepository.findByMember_MemberIdAndKeywordCategory(member.getMemberId(), request.getCategory());
        List<Integer> existingKeywordIds = existingMemberKeywords.stream()
                .map(memberKeyword -> memberKeyword.getKeyword().getKeywordId())
                .toList();

        // 이전 키워드와 비교하여 동일한지 확인
        if (new HashSet<>(existingKeywordIds).equals(new HashSet<>(request.getKeywords()))) {
            throw new GeneralException(ErrorStatus.NO_CHANGES_IN_KEYWORDS);
        }

        memberKeywordRepository.deleteByMember_MemberIdAndKeywordCategory(member.getMemberId(), category);


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

    // 멤버 검증 로직
    private Member validateMember(CustomUserPrincipal userPrincipal) {
        return memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    // 닉네임 검증 로직
    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_NICKNAME);
        }
        if (nickname.length() > 20) {
            throw new GeneralException(ErrorStatus.NICKNAME_TOO_LONG);
        }
    }
}
