package com.edison.project.domain.member.repository;

import com.edison.project.domain.member.entity.MemberMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberMemoryRepository extends JpaRepository<MemberMemory, Long> {

    // 메모리 문자열만 반환
    @Query("SELECT m.memory FROM MemberMemory m WHERE m.member.memberId = :memberId ORDER BY m.createdAt DESC")
    List<String> findMemoriesByMemberId(@Param("memberId") Long memberId);

    // MemberMemory 엔티티 자체를 반환
    @Query("SELECT m FROM MemberMemory m WHERE m.member.memberId = :memberId ORDER BY m.createdAt DESC")
    List<MemberMemory> findMemberMemoriesByMemberId(@Param("memberId") Long memberId);

    // 키워드 삭제 쿼리 추가
    @Modifying
    @Query("DELETE FROM MemberMemory m WHERE m.member.memberId = :memberId AND m.memory = :memory")
    int deleteByMemberIdAndMemory(@Param("memberId") Long memberId, @Param("memory") String memory);

}

