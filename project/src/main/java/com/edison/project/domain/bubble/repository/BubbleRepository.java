package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.Bubble;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BubbleRepository extends JpaRepository<Bubble, Long> {

    // 삭제되지 않은 Bubble만 조회
    Optional<Bubble> findByBubbleIdAndIsDeletedFalse(Long bubbleId);

    // 휴지통에 있는 Bubble만 조회
    Optional<Bubble> findByBubbleIdAndIsDeletedTrue(Long bubbleId);

    Page<Bubble> findByMember_MemberIdAndIsDeletedFalse(Long memberId, Pageable pageable);
    Page<Bubble> findByMember_MemberIdAndIsDeletedTrue(Long memberId, Pageable pageable);

    // 7일 이내 버블 목록
    @Query("SELECT b from Bubble b where b.member.memberId = :memberId AND b.isDeleted = false AND b.updatedAt >= :startDate")
    Page <Bubble> findRecentBubblesByMember(
            @Param("memberId") Long memberId,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable
    );

    // 전체 버블 검색
    @Query("SELECT b FROM Bubble b " +
            "WHERE (b.title LIKE %:keyword% OR b.content LIKE %:keyword% " +
            "OR EXISTS (SELECT 1 FROM BubbleLabel bl WHERE bl.bubble = b AND bl.label.name LIKE %:keyword%)) " +
            "AND b.isDeleted = false")
    List<Bubble> searchBubblesByKeyword(@Param("keyword") String keyword);

    // 30일 지난 휴지통 버블 목록
    @Query("SELECT b from Bubble b where b.updatedAt < :expiryDate and b.isDeleted = true")
    List<Bubble> findAllByUpdatedAtBeforeAndIsDeletedTrue(@Param("expiryDate") LocalDateTime expiryDate);
}
