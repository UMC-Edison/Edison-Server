package com.edison.project.domain.scrap.repository;

import com.edison.project.domain.artletter.dto.CountDto;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import com.edison.project.domain.member.entity.Member;
import com.edison.project.domain.scrap.entity.Scrap;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    int countByArtletter(Artletter artletter);
    boolean existsByMemberAndArtletter(Member member, Artletter artletter);

    @Modifying
    @Transactional
    void deleteByMemberAndArtletter(Member member, Artletter artletter);

    Page<Scrap> findByMember(Member member, Pageable pageable);

    Page<Scrap> findByMemberAndArtletter_Category(Member member, ArtletterCategory category, Pageable pageable);

    List<Scrap> findByMemberAndArtletterIn(Member member, List<Artletter> artletters);

    @Query("SELECT new com.edison.project.domain.artletter.dto.CountDto(a.artletter.letterId, COUNT(a)) " +
            "FROM ArtletterLikes a WHERE a.artletter IN :artletters GROUP BY a.artletter")
    List<CountDto> countByArtletterIn(@Param("artletters") List<Artletter> artletters);
}
