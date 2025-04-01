package com.edison.project.global.common.s3;

import com.edison.project.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/*
- 버킷을 버블 / 아트레터 2개의 폴더로 구분하여 S3에 업로드
- 버블 이미지 업로드: POST /upload/bubble (form-data: image 필드로 사진)
- 아트레터 이미지 업로드: POST /upload/artletter
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/upload")
public class FileUploadController {

    private final AmazonS3Manager amazonS3Manager;

    @PostMapping("/{type}")
    public ResponseEntity<String> uploadImage(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable("type") String type,
            @RequestParam("image") MultipartFile image
    ) {
        Long nickname = userPrincipal.getMemberId();
        String url = amazonS3Manager.uploadFile(type, nickname, image);
        return ResponseEntity.ok(url);
    }
}
