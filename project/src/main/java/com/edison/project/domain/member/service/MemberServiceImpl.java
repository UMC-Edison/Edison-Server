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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edison.project.domain.member.entity.Member;


import java.util.*;
import java.util.stream.Collectors;

import static com.edison.project.common.status.SuccessStatus._OK;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final MemberKeywordRepository memberKeywordRepository;
    private final KeywordsRepository keywordsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final RedisTokenService redisTokenService;

    @Override
    @Transactional
    public MemberResponseDto.LoginResultDto generateTokensForOidcUser(String email) {

        if (!memberRepository.existsByEmail(email)){
            Long memberId = createUserIfNotExist(email);
            String accessToken = jwtUtil.generateAccessToken(memberId, email);
            String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

            RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
            refreshTokenRepository.save(tokenEntity);

            return MemberResponseDto.LoginResultDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }
        else{
            // 이미 존재하는 사용자의 경우
            Long memberId = memberRepository.findByEmail(email).get().getMemberId();
            String accessToken = jwtUtil.generateAccessToken(memberId, email);
            String refreshToken = jwtUtil.generateRefreshToken(memberId, email);

            refreshTokenRepository.deleteByEmail(email);
            RefreshToken tokenEntity = RefreshToken.create(email, refreshToken);
            refreshTokenRepository.save(tokenEntity);

             return MemberResponseDto.LoginResultDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        }

    }

    @Override
    @Transactional
    public Long createUserIfNotExist(String email) {
        return memberRepository.findByEmail(email)
                .map(Member::getMemberId)
                .orElseGet(() -> {
                    Member member = Member.builder()
                            .email(email)
                            .build();
                    memberRepository.save(member);
                    return member.getMemberId();
                });
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> registerMember(CustomUserPrincipal userPrincipal,  MemberRequestDto.ProfileDto request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if(member.getNickname()!=null){
            throw new GeneralException(ErrorStatus.NICKNAME_ALREADY_SET);
        }

        if (request.getNickname()==null || request.getNickname() == "") {
            throw new GeneralException(ErrorStatus.NICKNAME_NOT_EXIST);
        }

        if (request.getNickname().length() > 20) {
            throw new GeneralException(ErrorStatus.NICKNAME_TOO_LONG);
        }

        member = member.registerProfile(request.getNickname());
        memberRepository.save(member);

        MemberResponseDto.ProfileResultDto response = MemberResponseDto.ProfileResultDto.builder()
                .nickname(member.getNickname())
                .build();

        return ApiResponse.onSuccess(SuccessStatus._OK, response);

    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateProfile(CustomUserPrincipal userPrincipal, MemberRequestDto.UpdateProfileDto request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (request.getNickname()==null || request.getNickname() == "") {
            throw new GeneralException(ErrorStatus.NICKNAME_NOT_EXIST);
        }

        if(Objects.equals(member.getNickname(), request.getNickname()) && request.getImageUrl()==null){
            throw new GeneralException(ErrorStatus.NICKNAME_NOT_CHANGED);
        }

        if(Objects.equals(member.getNickname(), request.getNickname()) && Objects.equals(member.getProfileImg(), request.getImageUrl())){
            throw new GeneralException(ErrorStatus.PROFILE_NOT_CHANGED);
        }

        MemberResponseDto.UpdateProfileResultDto response;

        if(request.getImageUrl()==null){
            member.updateNickname(request.getNickname());
            response = MemberResponseDto.UpdateProfileResultDto.builder()
                    .nickname(member.getNickname())
                    .imageUrl(member.getProfileImg())
                    .build();
        }
        else{
            member.updateProfile(request.getNickname(), request.getImageUrl());

            response = MemberResponseDto.UpdateProfileResultDto.builder()
                    .nickname(member.getNickname())
                    .imageUrl(request.getImageUrl())
                    .build();
        }

        return ApiResponse.onSuccess(SuccessStatus._OK, response);

    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> logout(CustomUserPrincipal userPrincipal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        String token = (String) authentication.getCredentials();
        long ttl = jwtUtil.getRemainingTime(token);

        // 블랙리스트에 추가
        redisTokenService.addToBlacklist(token, ttl);

        refreshTokenRepository.deleteByEmail(userPrincipal.getEmail());

        return ApiResponse.onSuccess(_OK);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> refreshAccessToken(String refreshToken) {

        Long memberId = jwtUtil.extractUserId(refreshToken);
        String email = jwtUtil.extractEmail(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(memberId, email);

        MemberResponseDto.RefreshResultDto response = MemberResponseDto.RefreshResultDto.builder()
                .accessToken(newAccessToken)
                .build();

        return ApiResponse.onSuccess(SuccessStatus._OK, response);

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
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> cancel(CustomUserPrincipal userPrincipal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Long memberId = userPrincipal.getMemberId();
        memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        // access토큰 블랙리스트에 추가
        String token = (String) authentication.getCredentials();
        redisTokenService.addToBlacklist(token,jwtUtil.getRemainingTime(token));

        // refresh토큰 삭제
        refreshTokenRepository.deleteByEmail(userPrincipal.getEmail());

        //member 삭제
        memberRepository.deleteByMemberId(memberId);

        return ApiResponse.onSuccess(_OK);
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

        List<MemberKeyword> existingMemberKeywords = memberKeywordRepository.findByMember_MemberIdAndKeywordCategory(member.getMemberId(), request.getCategory());
        List<Integer> existingKeywordIds = existingMemberKeywords.stream()
                .map(memberKeyword -> memberKeyword.getKeyword().getKeywordId())
                .collect(Collectors.toList());

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


}
