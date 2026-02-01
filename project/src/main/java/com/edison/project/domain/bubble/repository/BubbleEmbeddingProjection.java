package com.edison.project.domain.bubble.repository;

import java.time.LocalDateTime;

public interface BubbleEmbeddingProjection {
    String getLocalIdx();
    String getTitle();
    Double getEmbedding2dX();
    Double getEmbedding2dY();
    LocalDateTime getCreatedAt();
}