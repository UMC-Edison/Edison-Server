package com.edison.project.domain.artletter.service;

import com.edison.project.common.exception.GeneralException;
import com.edison.project.common.response.PageInfo;
import com.edison.project.common.status.ErrorStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.ArtletterLikes;
import com.edison.project.domain.artletter.repository.ArtletterLikesRepository;
import com.edison.project.domain.artletter.repository.ArtletterRepository;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.member.repository.MemberRepository;
import com.edison.project.domain.scrap.entity.Scrap;
import com.edison.project.domain.scrap.repository.ScrapRepository;
import com.edison.project.global.security.CustomUserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ArtletterServiceImpl implements ArtletterService {

    private final ArtletterRepository artletterRepository;
    private final MemberRepository memberRepository;
    private final ArtletterLikesRepository artletterLikesRepository;
    private final ScrapRepository scrapRepository;

    public ArtletterServiceImpl(ArtletterRepository artletterRepository, MemberRepository memberRepository, ArtletterLikesRepository artletterLikesRepository, ScrapRepository scrapRepository) {
        this.artletterRepository = artletterRepository;
        this.memberRepository = memberRepository;
        this.artletterLikesRepository = artletterLikesRepository;
        this.scrapRepository = scrapRepository;
    }

    public Page<Artletter> getAllArtletters(int page, int size) {
        // 페이지 요청 생성
        Pageable pageable = PageRequest.of(page, size);

        return artletterRepository.findAll(pageable);
    }


    @Override
    public ArtletterDTO.CreateResponseDto createArtletter(ArtletterDTO.CreateRequestDto request) {
        Artletter artletter = Artletter.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .writer(request.getWriter())
                .readTime(request.getReadTime())
                .tag(request.getTag())
                .category(request.getCategory())
                .build();

        Artletter savedArtletter = artletterRepository.save(artletter); // JpaRepository 기본 메서드 사용

        return ArtletterDTO.CreateResponseDto.builder()
                .artletterId(savedArtletter.getLetterId())
                .title(savedArtletter.getTitle())
                .build();
    }

    @Override
    @Transactional
    public ArtletterDTO.LikeResponseDto likeToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Artletter artletter = artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));

        boolean alreadyLiked = artletterLikesRepository.existsByMemberAndArtletter(member, artletter);

        if (alreadyLiked) {
            // 좋아요 취소
            artletterLikesRepository.deleteByMemberAndArtletter(member, artletter);
        } else {
            // 좋아요
            ArtletterLikes like = ArtletterLikes.builder()
                    .member(member)
                    .artletter(artletter)
                    .build();

            artletterLikesRepository.save(like);
        }

        int likeCnt = artletterLikesRepository.countByArtletter(artletter);

        return ArtletterDTO.LikeResponseDto.builder()
                .artletterId(letterId)
                .likesCnt(likeCnt)
                .isLiked(!alreadyLiked)
                .build();
    }

    @Override
    public ArtletterDTO.ScrapResponseDto scrapToggleArtletter(CustomUserPrincipal userPrincipal, Long letterId) {
        if (userPrincipal == null) {
            throw new GeneralException(ErrorStatus.LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(userPrincipal.getMemberId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        Artletter artletter = artletterRepository.findById(letterId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LETTERS_NOT_FOUND));

        boolean alreadyScrapped = scrapRepository.existsByMemberAndArtletter(member, artletter);

        if (alreadyScrapped) {
            scrapRepository.deleteByMemberAndArtletter(member, artletter);
        } else {
            Scrap scrap = Scrap.builder()
                    .member(member)
                    .artletter(artletter)
                    .build();

            scrapRepository.save(scrap);
        }

        int scrapCnt = scrapRepository.countByArtletter(artletter);

        return ArtletterDTO.ScrapResponseDto.builder()
                .artletterId(letterId)
                .scrapsCnt(scrapCnt)
                .isScrapped(!alreadyScrapped)
                .build();
    }

    @Override
    public Page<Artletter> searchArtletters(String keyword, Pageable pageable) {
        return artletterRepository.searchByKeyword(keyword, pageable);
    }

}
