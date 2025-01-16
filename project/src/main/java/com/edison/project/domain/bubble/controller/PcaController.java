package com.edison.project.domain.bubble.controller;

import com.edison.project.domain.bubble.dto.PcaRequestDto;
import com.edison.project.domain.bubble.service.PcaReducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pca")
@RequiredArgsConstructor
public class PcaController {

    private final PcaReducer pcaReducer;

    @PostMapping("/reduce")
    public Map<String, Object> reduceData(@RequestBody PcaRequestDto requestDto) {
        double[][] data = requestDto.getData();
        int[] labels = requestDto.getLabels();

        return pcaReducer.getReducedDataSet(data, labels);
    }
}