package com.edison.project.domain.space.repository;

import com.edison.project.domain.space.entity.MemberSpace;
import com.edison.project.domain.space.entity.Space;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberSpaceRepository extends JpaRepository<MemberSpace, Long> {

    // 특정 사용자가 소유한 Space 조회
    @Query("SELECT ms.space FROM MemberSpace ms WHERE ms.member.memberId = :memberId")
    List<Space> findSpacesByMemberId(@Param("memberId") Long memberId);

    Optional<MemberSpace> findByMember_MemberIdAndSpace_Id(Long memberId, Long spaceId);
}
