package com.edison.project.domain.scrap.repository;

import com.edison.project.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    Integer countByArtletter_LetterId(Long letterId);
    Boolean existsByArtletter_LetterIdAndMember_MemberId(Long artletterId, Long memberId);
}
