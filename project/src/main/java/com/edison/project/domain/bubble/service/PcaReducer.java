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
import com.edison.project.domain.bubble.entity.BubbleLabel;
import com.edison.project.domain.label.entity.Label;

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
        // Fetch all bubbles from the database
        List<Bubble> bubbles = bubbleRepository.findAll();

        // Map bubble IDs to their associated labels
        Map<Long, List<String>> bubbleLabels = bubbleLabelRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        bubbleLabel -> bubbleLabel.getBubble().getBubbleId(),
                        Collectors.mapping(
                                bubbleLabel -> bubbleLabel.getLabel().getName(),
                                Collectors.toList()
                        )
                ));

        // Combine title, content, and labels into a single text for each bubble
        List<String> bubbleTexts = bubbles.stream()
                .map(bubble -> {
                    String labels = String.join(" ", bubbleLabels.getOrDefault(bubble.getBubbleId(), List.of()));
                    return bubble.getTitle() + " " + bubble.getContent() + " " + labels;
                })
                .collect(Collectors.toList());

        // Compute TF-IDF matrix (this part assumes you have a utility for TF-IDF computation)
        double[][] tfIdfMatrix = computeTfIdfMatrix(bubbleTexts);

        // Perform PCA to reduce the TF-IDF matrix to 2D
        return performPca(tfIdfMatrix);
    }

    private double[][] computeTfIdfMatrix(List<String> texts) {
        // Step 1: Tokenize the text and build a vocabulary
        List<List<String>> tokenizedTexts = texts.stream()
                .map(text -> List.of(text.split("\\s+"))) // Split by whitespace
                .collect(Collectors.toList());

        // Flatten tokens and create a vocabulary (unique terms)
        List<String> vocabulary = tokenizedTexts.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        int numDocuments = texts.size();
        int numTerms = vocabulary.size();

        // Step 2: Term Frequency (TF) matrix
        double[][] tfMatrix = new double[numDocuments][numTerms];
        for (int docIndex = 0; docIndex < numDocuments; docIndex++) {
            List<String> tokens = tokenizedTexts.get(docIndex);
            for (int termIndex = 0; termIndex < numTerms; termIndex++) {
                String term = vocabulary.get(termIndex);
                long termCount = tokens.stream().filter(token -> token.equals(term)).count();
                tfMatrix[docIndex][termIndex] = (double) termCount / tokens.size();
            }
        }

        // Step 3: Inverse Document Frequency (IDF)
        double[] idfVector = new double[numTerms];
        for (int termIndex = 0; termIndex < numTerms; termIndex++) {
            String term = vocabulary.get(termIndex);
            long docsWithTerm = tokenizedTexts.stream()
                    .filter(tokens -> tokens.contains(term))
                    .count();
            idfVector[termIndex] = Math.log((double) numDocuments / (1 + docsWithTerm));
        }

        // Step 4: Compute TF-IDF matrix
        double[][] tfIdfMatrix = new double[numDocuments][numTerms];
        for (int docIndex = 0; docIndex < numDocuments; docIndex++) {
            for (int termIndex = 0; termIndex < numTerms; termIndex++) {
                tfIdfMatrix[docIndex][termIndex] = tfMatrix[docIndex][termIndex] * idfVector[termIndex];
            }
        }

        return tfIdfMatrix;
    }
}
