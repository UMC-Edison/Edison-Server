package com.edison.project.domain.label.repository;

import com.edison.project.domain.label.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {
    // 특정 사용자의 모든 라벨 조회(라벨만 있고 그 안에 버블 없는 경우를 위해)
    List<Label> findByMember_MemberId(Long memberId);
}
