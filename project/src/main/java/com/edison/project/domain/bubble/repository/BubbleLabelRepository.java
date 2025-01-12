package com.edison.project.domain.bubble.repository;

import com.edison.project.domain.bubble.entity.BubbleLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BubbleLabelRepository extends JpaRepository<BubbleLabel, Long> {

}
