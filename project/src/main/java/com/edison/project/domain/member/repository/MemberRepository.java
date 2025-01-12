package com.edison.project.domain.member.repository;

import com.edison.project.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 추가 쿼리 메서드는 여기 작성
    Optional<Member> findByEmail(String email);
}
