package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.BubbleLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BubbleLabelRepository extends JpaRepository<BubbleLabel, Long> {

    // 특정 버블에 연결된 모든 라벨 조회
    @Transactional(readOnly = true)
    List<BubbleLabel> findByBubble_BubbleId(Long bubbleId);

    // 특정 라벨을 가진 모든 버블 조회
    @Transactional(readOnly = true)
    List<BubbleLabel> findByLabel_LabelId(Long labelId);
}
