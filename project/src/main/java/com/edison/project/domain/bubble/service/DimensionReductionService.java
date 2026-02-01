package com.edison.project.domain.bubble.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import smile.feature.extraction.PCA;

@Service
@Slf4j
public class DimensionReductionService {

    private static final int EXPECTED_EMBEDDING_DIM = 1536; // OpenAI text-embedding-3-small

    /**
     * PCA를 사용하여 고차원 벡터를 2차원으로 축소 (Smile 라이브러리)
     */
    public double[][] reduceTo2D(float[][] vectors) {
        if (vectors == null || vectors.length == 0) {
            return new double[0][2];
        }
        if (vectors.length == 1) {
            return new double[][]{{0.0, 0.0}};
        }
        if (vectors.length == 2) {
            return new double[][]{{-0.5, 0.0}, {0.5, 0.0}};
        }

        try {
            double[][] data = toDoubleArray(vectors);

            // Smile PCA: 2차원으로 축소
            PCA pca = PCA.fit(data).getProjection(2);
            double[][] result = new double[data.length][2];

            for (int i = 0; i < data.length; i++) {
                result[i] = pca.apply(data[i]);
            }

            return result;

        } catch (Exception e) {
            log.error("PCA reduction failed, falling back to random projection", e);
            return createRandomProjection(vectors.length);
        }
    }

    private double[][] toDoubleArray(float[][] vectors) {
        if (vectors == null || vectors.length == 0) {
            return new double[0][0];
        }

        int n = vectors.length;
        int d = vectors[0].length;

        // 간단한 검증만 추가 (선택사항)
        if (d != EXPECTED_EMBEDDING_DIM) {
            log.warn("Unexpected embedding dimension: expected {}, got {}",
                    EXPECTED_EMBEDDING_DIM, d);
        }

        double[][] data = new double[n][d];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < d; j++) {
                data[i][j] = vectors[i][j];
            }
        }
        return data;
    }

    private double[][] createRandomProjection(int size) {
        double[][] projection = new double[size][2];
        for (int i = 0; i < size; i++) {
            projection[i][0] = (Math.random() - 0.5) * 2.0;
            projection[i][1] = (Math.random() - 0.5) * 2.0;
        }
        return projection;
    }
}