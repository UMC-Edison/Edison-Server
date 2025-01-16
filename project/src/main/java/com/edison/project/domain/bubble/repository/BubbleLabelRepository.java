package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.entity.BubbleLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BubbleLabelRepository extends JpaRepository<BubbleLabel, Long> {

    // 특정 라벨에 연결된 모든 버블(삭제되지 않은) 조회
    @Query("SELECT b " +
            "FROM BubbleLabel bl " +
            "JOIN bl.bubble b " +
            "WHERE bl.label.labelId = :labelId AND b.isDeleted = false")
    List<Bubble> findBubblesByLabelId(@Param("labelId") Long labelId);

}
