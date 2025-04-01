package com.edison.project.global.common.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.edison.project.aws.s3.AmazonS3Manager;

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
            @PathVariable("type") String type,
            @RequestParam("image") MultipartFile file
    ) {
        if (!type.equals("bubble") && !type.equals("artletter")) {
            return ResponseEntity.badRequest().body("지원하지 않습니다.");
        }

        String folder = type.equals("bubble") ? "bubble-images" : "artletter-images";
        String imageUrl = amazonS3Manager.uploadFile(folder, file);
        return ResponseEntity.ok(imageUrl);
    }
}
