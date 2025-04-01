package com.edison.project.global.common.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.edison.project.global.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmazonS3Manager {

    private final AmazonS3 amazonS3;
    private final AmazonConfig amazonConfig;

    public String uploadFile(String type, Long username, MultipartFile file) {
        String folder = switch (type) {
            case "bubble" -> "bubble-images";
            case "artletter" -> "artletter-images";
            default -> throw new IllegalArgumentException("지원하지 않는 타입입니다.");
        };

        String fileName = folder + "/" + username + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(amazonConfig.getBucket(), fileName, file.getInputStream(), metadata);
            return amazonS3.getUrl(amazonConfig.getBucket(), fileName).toString();
        } catch (IOException e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }
}
