package com.edison.project.domain.bubble.service;

import com.pgvector.PGvector;

public interface EmbeddingService {
    /**
     * 주어진 텍스트를 임베딩 벡터로 변환
     * @param text 임베딩할 텍스트
     * @return PGvector 형태의 임베딩 벡터
     */
    PGvector embed(String text);

    /**
     * 복수의 텍스트를 임베딩 벡터로 변환
     * @param texts 임베딩할 텍스트 목록
     * @return PGvector 배열
     */
    PGvector[] embedBatch(String[] texts);
}
