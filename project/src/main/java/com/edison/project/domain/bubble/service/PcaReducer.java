package com.edison.project.domain.bubble.service;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.edison.project.domain.bubble.repository.BubbleRepository;
import com.edison.project.domain.bubble.repository.BubbleLabelRepository;
import com.edison.project.domain.label.repository.LabelRepository;
import com.edison.project.domain.bubble.entity.Bubble;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PcaReducer {

    private final BubbleRepository bubbleRepository;
    private final BubbleLabelRepository bubbleLabelRepository;
    private final LabelRepository labelRepository;

    @Autowired
    public PcaReducer(BubbleRepository bubbleRepository, BubbleLabelRepository bubbleLabelRepository, LabelRepository labelRepository) {
        this.bubbleRepository = bubbleRepository;
        this.bubbleLabelRepository = bubbleLabelRepository;
        this.labelRepository = labelRepository;
    }

    public double[][] performPca(double[][] tfIdfMatrix) {
        RealMatrix matrix = MatrixUtils.createRealMatrix(tfIdfMatrix);
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
        RealMatrix u = svd.getU();
        return u.getSubMatrix(0, u.getRowDimension() - 1, 0, 1).getData();
    }

    public double[][] reduceAllBubblesTo2D() {
        // 버블 전부 가져오기
        List<Bubble> bubbles = bubbleRepository.findAll();

        // 버블 id를 label과 mapping
        Map<Long, List<String>> bubbleLabels = bubbleLabelRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        bubbleLabel -> bubbleLabel.getBubble().getBubbleId(),
                        Collectors.mapping(
                                bubbleLabel -> bubbleLabel.getLabel().getName(),
                                Collectors.toList()
                        )
                ));

        // title, content, and labels을 묶어서 text 만들기
        List<String> bubbleTexts = bubbles.stream()
                .map(bubble -> {
                    String labels = String.join(" ", bubbleLabels.getOrDefault(bubble.getBubbleId(), List.of()));
                    return bubble.getTitle() + " " + bubble.getContent() + " " + labels;
                })
                .collect(Collectors.toList());

        // TF-IDF matrix 연산
        double[][] tfIdfMatrix = computeTfIdfMatrix(bubbleTexts);

        return performPca(tfIdfMatrix);
    }

    private double[][] computeTfIdfMatrix(List<String> texts) {
        // Step 1: text tokenizing 하기
        List<List<String>> tokenizedTexts = texts.stream()
                .map(text -> List.of(text.split("\\s+"))) // Split by whitespace
                .collect(Collectors.toList());

        // 토큰 flatten, 어휘(고유 용어) 생성하기
        List<String> vocabulary = tokenizedTexts.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        int numDocuments = texts.size();
        int numTerms = vocabulary.size();

        // Step 2: Term Frequency (TF) matrix 연산
        double[][] tfMatrix = new double[numDocuments][numTerms];
        for (int docIndex = 0; docIndex < numDocuments; docIndex++) {
            List<String> tokens = tokenizedTexts.get(docIndex);
            for (int termIndex = 0; termIndex < numTerms; termIndex++) {
                String term = vocabulary.get(termIndex);
                long termCount = tokens.stream().filter(token -> token.equals(term)).count();
                tfMatrix[docIndex][termIndex] = (double) termCount / tokens.size();
            }
        }

        // Step 3: Inverse Document Frequency (IDF) 연산
        double[] idfVector = new double[numTerms];
        for (int termIndex = 0; termIndex < numTerms; termIndex++) {
            String term = vocabulary.get(termIndex);
            long docsWithTerm = tokenizedTexts.stream()
                    .filter(tokens -> tokens.contains(term))
                    .count();
            idfVector[termIndex] = Math.log((double) numDocuments / (1 + docsWithTerm));
        }

        // Step 4: TF-IDF matrix 연산
        double[][] tfIdfMatrix = new double[numDocuments][numTerms];
        for (int docIndex = 0; docIndex < numDocuments; docIndex++) {
            for (int termIndex = 0; termIndex < numTerms; termIndex++) {
                tfIdfMatrix[docIndex][termIndex] = tfMatrix[docIndex][termIndex] * idfVector[termIndex];
            }
        }

        return tfIdfMatrix;
    }
}
