package com.edison.project.domain.member.repository;

import com.edison.project.domain.member.entity.MemberKeyword;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberKeywordRepository extends JpaRepository<MemberKeyword, Long> {

    @Modifying
    @Query("DELETE FROM MemberKeyword mk WHERE mk.member.memberId = :memberId AND mk.keyword.category = :category")
    void deleteByMember_MemberIdAndKeyword_Category(Long memberId, String category);

    // 특정 카테고리에 대한 키워드가 이미 저장되어 있는지 확인
    boolean existsByMember_MemberIdAndKeyword_Category(Long memberId, String category);

    boolean existsByMember_MemberIdAndKeyword_KeywordId(Long memberId, Integer keywordId);

    @Query("SELECT mk FROM MemberKeyword mk WHERE mk.member.id = :memberId AND mk.keyword.category = :category")
    List<MemberKeyword> findByMemberIdAndKeywordCategory(@Param("memberId") Long memberId, @Param("category") String category);

    void deleteByMember_MemberIdAndKeywordCategory(Long memberId, String category);
}
