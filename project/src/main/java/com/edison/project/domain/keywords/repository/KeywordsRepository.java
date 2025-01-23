package com.edison.project.domain.keywords.repository;

import com.edison.project.domain.keywords.entity.Keywords;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KeywordsRepository extends JpaRepository<Keywords, Integer> {
    List<Keywords> findAllByCategory(String category);
    List<Keywords> findAllByOrderByCategoryAsc();
}

