package com.edison.project.domain.member.repository;

import com.edison.project.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByMemberId(Long memberId);
    void deleteByMemberId(Long memeberId);
    Member findByMemberId(Long memberId);
}
