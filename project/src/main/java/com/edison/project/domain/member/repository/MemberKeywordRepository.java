package com.edison.project.domain.member.repository;

import com.edison.project.domain.member.entity.MemberKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MemberKeywordRepository extends JpaRepository<MemberKeyword, Long> {

    // 사용자별 특정 카테고리에 키워드 존재 여부 확인
    boolean existsByMember_MemberIdAndKeyword_Category(Long memberId, String category);
}
