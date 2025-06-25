package com.edison.project.domain.label.repository;

import com.edison.project.domain.label.entity.Label;
import com.edison.project.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LabelRepository extends JpaRepository<Label, String> {

    // 특정 사용자의 모든 라벨 정보와 & 라벨별 버블 개수 조회
    @Query("SELECT l, COUNT(bl.bubble) " +
            "FROM Label l " +
            "LEFT JOIN BubbleLabel bl ON l.labelId = bl.label.labelId AND bl.bubble.isTrashed = false " +
            "WHERE l.member.memberId = :memberId " +
            "GROUP BY l")
    List<Object[]> findLabelInfoByMemberId(@Param("memberId") Long memberId);

    Set<Label> findAllByMemberAndLocalIdxIn(Member member, Set<String> localIdxs);

    Optional<Label> findLabelByMemberAndLocalIdx(Member member, String localIdx);

    boolean existsByMemberAndLocalIdx(Member member, String localIdx);
}
