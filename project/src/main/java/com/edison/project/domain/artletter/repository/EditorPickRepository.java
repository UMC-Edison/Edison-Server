package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.artletter.entity.Artletter;
import com.edison.project.domain.artletter.entity.EditorPick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EditorPickRepository extends JpaRepository<EditorPick, Long>, ArtletterRepositoryCustom {
    List<EditorPick> findAll();
}