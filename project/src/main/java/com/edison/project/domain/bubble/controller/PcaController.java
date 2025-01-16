package com.edison.project.domain.bubble.controller;

import com.edison.project.domain.bubble.service.PcaReducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pca")
@RequiredArgsConstructor
public class PcaController {

    private final PcaReducer pcaReducer;

    @GetMapping("/reduce-all")
    public double[][] reduceAllBubblesTo2D() {
        // Fetch all data from the database, compute TF-IDF, and apply PCA
        return pcaReducer.reduceAllBubblesTo2D();
    }
}