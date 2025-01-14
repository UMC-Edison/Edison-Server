package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.BubbleLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BubbleLabelRepository extends JpaRepository<BubbleLabel, Long> {
    // 라벨별로 버블의 개수를 가져오는 쿼리
    @Query("SELECT bl.label, COUNT(bl.bubble) FROM BubbleLabel bl " +
            "WHERE bl.label.member.memberId = :memberId " +
            "GROUP BY bl.label")
    List<Object[]> findBubbleCountsByMemberId(@Param("memberId") Long memberId);
}
