package com.example.project.domain.label.repository;

import com.example.project.domain.label.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, Long> {
    // 추가 쿼리 메서드는 여기 작성
}
