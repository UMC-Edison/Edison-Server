package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BubbleRepository extends JpaRepository<Bubble, Long> {

    // ============ 단건 조회 ============
    Optional<Bubble> findByMember_MemberIdAndLocalIdxAndIsTrashedFalse(Long memberId, String localIdx);
    Optional<Bubble> findByMember_MemberIdAndLocalIdxAndIsTrashedTrue(Long memberId, String localIdx);
    Optional<Bubble> findByMemberAndLocalIdx(Member member, String localIdx);

    // 단건 상세 조회 (연관관계 함께 로딩)
    @EntityGraph(attributePaths = {"labels", "labels.label", "backlinks", "backlinks.backlinkBubble"})
    @Query("SELECT b FROM Bubble b " +
            "WHERE b.member.memberId = :memberId " +
            "AND b.localIdx = :localIdx " +
            "AND b.isTrashed = false")
    Optional<Bubble> findByMemberAndLocalIdxWithDetails(
            @Param("memberId") Long memberId,
            @Param("localIdx") String localIdx
    );

    // ============ 목록 조회 (페이징) ============

    // 전체 버블 (휴지통 제외)
    Page<Bubble> findByMember_MemberIdAndIsTrashedFalse(Long memberId, Pageable pageable);

    // 휴지통 버블
    Page<Bubble> findByMember_MemberIdAndIsTrashedTrue(Long memberId, Pageable pageable);

    // 전체 버블 (휴지통 포함)
    Page<Bubble> findByMember_MemberId(Long memberId, Pageable pageable);

    // 최근 7일 버블
    @Query("SELECT b FROM Bubble b " +
            "WHERE b.member.memberId = :memberId " +
            "AND b.isTrashed = false " +
            "AND b.updatedAt >= :since")
    Page<Bubble> findRecentByMember(
            @Param("memberId") Long memberId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );

    // ============ 목록 조회 (리스트) ============
    List<Bubble> findByMember_MemberIdAndIsTrashedFalse(Long memberId);

    // ============ 배치 조회 ============
    Set<Bubble> findAllByMemberAndLocalIdxIn(Member member, Set<String> localIdxs);

    // ============ 존재 여부 ============
    Boolean existsByMemberAndLocalIdx(Member member, String localIdx);
}