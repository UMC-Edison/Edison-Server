package com.edison.project.domain.scrap.repository;

import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {
    int countByArtletter(Artletter artletter);
    boolean existsByMemberAndArtletter(Member member, Artletter artletter);
}
