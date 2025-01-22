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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edison.project.domain.member.entity.Member;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<ApiResponse> registerMember(CustomUserPrincipal userPrincipal, MemberResponseDto.ProfileResultDto request) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (request.getNickname()==null || request.getNickname() == "") {
            throw new GeneralException(ErrorStatus.NICKNAME_NOT_EXIST);
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
    public ResponseEntity<ApiResponse> refreshAccessToken(String token) {

        Long memberId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractEmail(token);

        RefreshToken refreshToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LOGIN_REQUIRED));

        if (jwtUtil.isTokenExpired(refreshToken.getRefreshToken())) {
            throw new GeneralException(ErrorStatus.REFRESHTOKEN_EXPIRED);
        }

        String newAccessToken = jwtUtil.generateAccessToken(memberId, email);

        // 전에 발급받은 access token 블랙리스트에 추가
        redisTokenService.addToBlacklist(token, jwtUtil.getRemainingTime(token));

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
        String category = request.getCategory();
        List<String> validCategories = List.of("CATEGORY1", "CATEGORY2", "CATEGORY3", "CATEGORY4");
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

        // 기존 키워드 삭제 (동일 카테고리에 한해 삭제)
        //memberKeywordRepository.deleteByMember_MemberIdAndKeyword_Category(member.getMemberId(), category);

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


}
