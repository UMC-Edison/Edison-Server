package com.edison.project.domain.member.repository;

import com.edison.project.domain.member.entity.MemberKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MemberKeywordRepository extends JpaRepository<MemberKeyword, Long> {

    // 사용자별 키워드 존재 여부 확인
    boolean existsByMember_MemberId(Long memberId);
}
