package com.example.project.domain.bubble.repository;

import com.example.project.domain.bubble.entity.Bubble;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BubbleRepository extends JpaRepository<Bubble, Long> {
    // 추가 쿼리 메서드는 여기 작성
}
