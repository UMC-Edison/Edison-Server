package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.member.entity.Member;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BubbleRepository extends JpaRepository<Bubble, Long> {

    // 삭제되지 않은 Bubble만 조회
    Optional<Bubble> findByMember_MemberIdAndLocalIdxAndIsTrashedFalse(Long memberId, Long localIdx);

    Page<Bubble> findByMember_MemberIdAndIsTrashedFalse(Long memberId, Pageable pageable);
    Page<Bubble> findByMember_MemberIdAndIsTrashedTrue(Long memberId, Pageable pageable);

    // 휴지통에 있는 버블 포함 조회
    Page<Bubble> findByMember_MemberId(Long memberId, Pageable pageable);

    // 7일 이내 버블 목록
    @Query("SELECT b from Bubble b where b.member.memberId = :memberId AND b.isDeleted = false AND b.updatedAt >= :startDate")
    Page <Bubble> findRecentBubblesByMember(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable
    );

    Set<Bubble> findAllByMemberAndLocalIdxIn(Member member, Set<Long> localIdxs);
    Optional<Bubble> findByMemberAndLocalIdx(Member member, Long localIdx);
    Boolean existsByMemberAndLocalIdx(Member member, Long localIdx);

    List<Bubble> findByMember_MemberIdAndIsTrashedTrue(Long memberId);

}
