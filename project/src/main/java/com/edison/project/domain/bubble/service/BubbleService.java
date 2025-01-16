package com.edison.project.domain.bubble.service;

import com.edison.project.domain.bubble.dto.BubbleRequestDto;
import com.edison.project.domain.bubble.dto.BubbleResponseDto;

import java.util.List;

public interface BubbleService {
    BubbleResponseDto.CreateResultDto createBubble(BubbleRequestDto.CreateDto requestDto);
    BubbleResponseDto.DeleteResultDto deleteBubble(BubbleRequestDto.DeleteDto requestDto);
    BubbleResponseDto.RestoreResultDto restoreBubble(BubbleRequestDto.RestoreDto requestDto);
    List<String> getCombinedTexts();
    double[][] calculateTfIdf(List<String> combinedTexts);
    double[][] performPca(double[][] tfIdfMatrix);
}
