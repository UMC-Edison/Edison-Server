package com.edison.project.domain.bubble.controller;

import com.edison.project.domain.bubble.service.PcaReducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SpaceController {

    private final PcaReducer pcaReducer;

    @GetMapping("/space")
    public double[][] reduceAllBubblesTo2D() {
        // 데이터 전부 가져와서 TF-IDF -> PCA
        return pcaReducer.reduceAllBubblesTo2D();
    }
}