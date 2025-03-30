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
        // Full-Text Search 기반 relevance 정렬
        String nativeQueryStr = """
            SELECT *, MATCH(tag, title, content) AGAINST (:keyword IN BOOLEAN MODE) AS relevance
            FROM artletter
            WHERE MATCH(tag, title, content) AGAINST (:keyword IN BOOLEAN MODE)
            ORDER BY relevance DESC
            LIMIT :limit OFFSET :offset
        """;

        String countQueryStr = """
            SELECT COUNT(*) FROM artletter
            WHERE MATCH(tag, title, content) AGAINST (:keyword IN BOOLEAN MODE)
        """;

        List<Artletter> resultList = entityManager.createNativeQuery(nativeQueryStr, Artletter.class)
                .setParameter("keyword", keyword + "*")  // 부분 매치 지원
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset())
                .getResultList();

        Number totalCount = (Number) entityManager.createNativeQuery(countQueryStr)
                .setParameter("keyword", keyword + "*")
                .getSingleResult();

        return new PageImpl<>(resultList, pageable, totalCount.longValue());
    }
}
