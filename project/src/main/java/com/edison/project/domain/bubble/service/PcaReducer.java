package com.edison.project.domain.bubble.service;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PcaReducer {

    public double[][] reduceTo2D(double[][] data) {
        // 데이터 공분산 행렬 계산
        RealMatrix matrix = MatrixUtils.createRealMatrix(data);
        Covariance covariance = new Covariance(matrix);
        RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();

        // 고유값 분해 수행
        EigenDecomposition eigenDecomposition = new EigenDecomposition(covarianceMatrix);

        // 가장 큰 두 고유값에 해당하는 고유벡터 선택
        double[][] eigenVectors = eigenDecomposition.getV().getData();
        double[] eigenValues = eigenDecomposition.getRealEigenvalues();

        // 고유값을 내림차순으로 정렬하고 인덱스 추적
        int[] indices = sortIndicesByDescendingOrder(eigenValues);

        // 첫 번째와 두 번째 고유벡터를 선택
        double[][] top2EigenVectors = new double[matrix.getColumnDimension()][2];
        for (int i = 0; i < matrix.getColumnDimension(); i++) {
            top2EigenVectors[i][0] = eigenVectors[i][indices[0]];
            top2EigenVectors[i][1] = eigenVectors[i][indices[1]];
        }

        // 데이터에 투영
        RealMatrix projectionMatrix = MatrixUtils.createRealMatrix(top2EigenVectors);
        RealMatrix reducedData = matrix.multiply(projectionMatrix);

        return reducedData.getData();
    }

    private int[] sortIndicesByDescendingOrder(double[] array) {
        Integer[] indices = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            indices[i] = i;
        }
        java.util.Arrays.sort(indices, (i, j) -> Double.compare(array[j], array[i]));
        return java.util.Arrays.stream(indices).mapToInt(Integer::intValue).toArray();
    }

    public Map<String, Object> getReducedDataSet(double[][] data, int[] labels) {
        double[][] reducedData = reduceTo2D(data);

        Map<String, Object> resultSet = new HashMap<>();
        resultSet.put("x", reducedData);
        resultSet.put("y", labels);

        return resultSet;
    }
}