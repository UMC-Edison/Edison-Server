package com.edison.project.global.common.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/s3")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/upload-url")
    public ResponseEntity<S3Service.PresignedUrlResponse> getPresignedUrl(@RequestParam String fileName) {
        return ResponseEntity.ok(s3Service.generatePresignedUrl(fileName));
    }

    @GetMapping("/get-img")
    public ResponseEntity<String> getPresignedDownloadUrl(@RequestParam String key) {
        String presignedUrl = s3Service.generatePresignedGetUrl(key);
        return ResponseEntity.ok(presignedUrl);
    }

}

