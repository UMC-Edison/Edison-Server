package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.artletter.entity.Artletter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtletterRepositoryCustom {
    Page<Artletter> searchByKeyword(String keyword, Pageable pageable);
}

