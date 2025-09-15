package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtletterRepository extends JpaRepository<Artletter, Long>, ArtletterRepositoryCustom {
    List<Artletter> findByLetterIdIn(List<Long> letterIds);

    Page<Artletter> findByCategory(ArtletterCategory category, Pageable pageable);

    @Query("SELECT a.letterId FROM Artletter a")
    List<Long> findAllIds();

    @Query("SELECT a FROM Artletter a WHERE a.keyword IN :keywords")
    List<Artletter> findByKeywordsIn(@Param("keywords") List<String> keywords);
}

