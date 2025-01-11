package com.edison.project.domain.artletter.controller;

import com.edison.project.common.response.ApiResponse;
import com.edison.project.common.status.SuccessStatus;
import com.edison.project.domain.artletter.dto.ArtletterDTO;
import com.edison.project.domain.artletter.service.ArtletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/artletters")
@RequiredArgsConstructor
public class ArtletterController {

    private final ArtletterService artletterService;

    // POST: Create Artletter
    @PostMapping
    public ResponseEntity<ApiResponse> createArtletter(@RequestBody @Valid ArtletterDTO.CreateRequestDto request) {
        ArtletterDTO.CreateResponseDto response = artletterService.createArtletter(request);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }

    // GET: Get All Artletters
    @GetMapping
    public ResponseEntity<ApiResponse> getAllArtletters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ArtletterDTO.ListResponseDto response = artletterService.getAllArtletters(page, size);
        return ApiResponse.onSuccess(SuccessStatus._OK, response);
    }
}
