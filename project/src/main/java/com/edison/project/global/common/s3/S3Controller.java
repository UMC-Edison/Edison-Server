package com.edison.project.global.common.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/upload-url")
    public ResponseEntity<S3Service.PresignedUrlResponse> getPresignedUrl(@RequestParam String fileName) {
        return ResponseEntity.ok(s3Service.generatePresignedUrl(fileName));
    }
}

