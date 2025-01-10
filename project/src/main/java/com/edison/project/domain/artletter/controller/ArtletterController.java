package com.edison.project.domain.artletter.controller;

import com.edison.project.domain.artletter.dto.TestDto;
import com.edison.project.domain.artletter.service.ArtletterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/artletters")
public class ArtletterController {

    private final ArtletterService artletterService;

    public ArtletterController(ArtletterService artletterService) {
        this.artletterService = artletterService;
    }

    @GetMapping
    public ResponseEntity<TestDto> getAllArtletters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        TestDto response = artletterService.getAllArtletters(page, size);
        return ResponseEntity.ok(response);
    }
}
