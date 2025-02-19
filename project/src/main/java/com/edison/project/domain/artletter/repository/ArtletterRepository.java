package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.artletter.dto.CountDto;
import com.edison.project.domain.artletter.entity.Artletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtletterRepository extends JpaRepository<Artletter, Long>, ArtletterRepositoryCustom {
    List<Artletter> findByLetterIdIn(List<Long> letterIds);
}

