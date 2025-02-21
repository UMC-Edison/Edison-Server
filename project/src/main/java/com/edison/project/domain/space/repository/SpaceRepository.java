package com.edison.project.domain.space.repository;

import com.edison.project.domain.space.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {
    @Query("SELECT s FROM Space s JOIN FETCH s.bubble WHERE s.bubble.bubbleId = :bubbleId AND s.memberId = :memberId")
    List<Space> findByBubble_BubbleIdAndMemberId(@Param("bubbleId") Long bubbleId, @Param("memberId") Long memberId);

    @Query("SELECT s FROM Space s WHERE s.memberId = :memberId")
    List<Space> findByMemberId(@Param("memberId") Long memberId);

    void deleteByBubble_BubbleIdIn(List<Long> bubbleIds);

}

