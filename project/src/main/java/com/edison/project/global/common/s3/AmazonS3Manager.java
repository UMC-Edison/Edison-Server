package com.edison.project.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.edison.project.global.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmazonS3Manager {

    private final AmazonS3 amazonS3;
    private final AmazonConfig amazonConfig;

    public String uploadFile(String keyName, MultipartFile file) {
        try {
            String fileName = keyName + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(
                    amazonConfig.getBucket(),
                    fileName,
                    file.getInputStream(),
                    metadata
            );

            return amazonS3.getUrl(amazonConfig.getBucket(), fileName).toString();
        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }
}
