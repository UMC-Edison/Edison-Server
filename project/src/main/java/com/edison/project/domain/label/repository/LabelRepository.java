package com.edison.project.domain.label.repository;

import com.edison.project.domain.label.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {

    // 특정 사용자의 모든 라벨 정보와 & 라벨별 버블 개수 조회
    @Query("SELECT l, COUNT(bl.bubble) " +
            "FROM Label l " +
            "LEFT JOIN BubbleLabel bl ON l.labelId = bl.label.labelId AND bl.bubble.isDeleted = false " +
            "WHERE l.member.memberId = :memberId " +
            "GROUP BY l")
    List<Object[]> findLabelInfoByMemberId(@Param("memberId") Long memberId);

    boolean existsByLabelId(Long labelId);
}
