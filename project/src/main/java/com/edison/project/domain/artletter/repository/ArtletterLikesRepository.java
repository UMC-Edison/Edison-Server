package com.edison.project.domain.artletter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.edison.project.domain.artletter.entity.ArtletterLikes;
import com.edison.project.domain.artletter.entity.Artletter;

@Repository
public interface ArtletterLikesRepository extends JpaRepository<ArtletterLikes, Long> {
    Integer countByArtletter_LetterId(Long letterId);
}
