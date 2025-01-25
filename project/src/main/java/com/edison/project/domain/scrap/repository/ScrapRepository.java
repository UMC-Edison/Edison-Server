package com.edison.project.domain.scrap.repository;

import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.scrap.entity.Scrap;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;


public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    int countByArtletter(Artletter artletter);
    boolean existsByMemberAndArtletter(Member member, Artletter artletter);

    @Modifying
    @Transactional
    void deleteByMemberAndArtletter(Member member, Artletter artletter);

    Page<Scrap> findByMember(Member member, Pageable pageable);
}
