package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.artletter.dto.CountDto;
import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.ArtletterCategory;
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
}

