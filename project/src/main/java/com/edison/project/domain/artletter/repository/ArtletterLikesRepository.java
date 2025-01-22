package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.edison.project.domain.artletter.entity.ArtletterLikes;
import com.edison.project.domain.artletter.entity.Artletter;

@Repository
public interface ArtletterLikesRepository extends JpaRepository<ArtletterLikes, Long> {

    int countByArtletter(Artletter artletter);
    boolean existsByMemberAndArtletter(Member member, Artletter artletter);

}
