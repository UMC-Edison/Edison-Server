package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.artletter.dto.CountDto;
import com.edison.project.domain.member.entity.Member;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.edison.project.domain.artletter.entity.ArtletterLikes;
import com.edison.project.domain.artletter.entity.Artletter;

import java.util.List;

@Repository
public interface ArtletterLikesRepository extends JpaRepository<ArtletterLikes, Long> {

    int countByArtletter(Artletter artletter);
    boolean existsByMemberAndArtletter(Member member, Artletter artletter);

    @Modifying
    @Transactional
    void deleteByMemberAndArtletter(Member member, Artletter artletter);

    List<ArtletterLikes> findByMemberAndArtletterIn(Member member, List<Artletter> artletters);

    @Query("SELECT new com.edison.project.domain.artletter.dto.CountDto(a.artletter.letterId, COUNT(a)) " +
            "FROM ArtletterLikes a WHERE a.artletter IN :artletters GROUP BY a.artletter")
    List<CountDto> countByArtletterIn(@Param("artletters") List<Artletter> artletters);


}
