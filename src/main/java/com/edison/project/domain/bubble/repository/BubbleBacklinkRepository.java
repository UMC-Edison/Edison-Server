package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.Bubble;
import com.edison.project.domain.bubble.entity.BubbleBacklink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BubbleBacklinkRepository extends JpaRepository<BubbleBacklink, Long> {
    List<BubbleBacklink> findByBacklinkBubble_BubbleId(Long bubbleId);

    List<BubbleBacklink> findByBubble_BubbleId(Long bubbleId);
}
