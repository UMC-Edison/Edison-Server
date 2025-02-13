package com.edison.project.domain.artletter.repository;

import com.edison.project.domain.artletter.entity.Artletter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class ArtletterRepositoryCustomImpl implements ArtletterRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Artletter> searchByKeyword(String keyword, Pageable pageable) {
        TypedQuery<Artletter> query = createSearchQuery(keyword);
        TypedQuery<Long> countQuery = createCountQuery(keyword);

        long totalRows = countQuery.getSingleResult();
        applyPagination(query, pageable);

        return new PageImpl<>(query.getResultList(), pageable, totalRows);
    }

    private TypedQuery<Artletter> createSearchQuery(String keyword) {
        String queryStr = "SELECT a FROM Artletter a WHERE a.title LIKE :keyword OR a.content LIKE :keyword";
        TypedQuery<Artletter> query = entityManager.createQuery(queryStr, Artletter.class);
        query.setParameter("keyword", "%" + keyword + "%");
        return query;
    }

    private TypedQuery<Long> createCountQuery(String keyword) {
        String countQueryStr = "SELECT COUNT(a) FROM Artletter a WHERE a.title LIKE :keyword OR a.content LIKE :keyword";
        TypedQuery<Long> countQuery = entityManager.createQuery(countQueryStr, Long.class);
        countQuery.setParameter("keyword", "%" + keyword + "%");
        return countQuery;
    }

    private void applyPagination(TypedQuery<Artletter> query, Pageable pageable) {
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
    }
}
