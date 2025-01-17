package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.Bubble;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BubbleRepository extends JpaRepository<Bubble, Long> {

    // 삭제되지 않은 Bubble만 조회
    Optional<Bubble> findByBubbleIdAndIsDeletedFalse(Long bubbleId);

    // 휴지통에 있는 Bubble만 조회
    Optional<Bubble> findByBubbleIdAndIsDeletedTrue(Long bubbleId);

    Page<Bubble> findByMember_MemberIdAndIsDeletedFalse(Long memberId, Pageable pageable);

}
