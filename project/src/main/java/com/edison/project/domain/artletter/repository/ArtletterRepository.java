package com.example.project.domain.artletter.repository;

import com.example.project.domain.bubble.entity.Artletter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtletterRepository extends JpaRepository<Member, Long> {
    // 추가 쿼리 메서드는 여기 작성
}
