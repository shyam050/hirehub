package com.hirehub.resume.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.ResponseInputStream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * AWS S3 implementation of FileStorageService.
 * Used in production via spring.profiles.active=prod and file.storage.type=s3.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    @Value("${file.storage.s3.bucket}")
    private String bucket;

    @Value("${file.storage.s3.region:us-east-1}")
    private String region;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        log.info("S3 file storage initialized for bucket: {} in region: {}", bucket, region);
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, String contentType) {
        String storageKey = generateStorageKey(originalFilename);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, -1)
            );

            log.debug("File stored in S3: key={}", storageKey);
            return storageKey;
        } catch (Exception e) {
            log.error("Failed to store file in S3: {}", e.getMessage());
            throw new RuntimeException("File storage failed", e);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .build()
            );
        } catch (NoSuchKeyException e) {
            log.warn("File not found in S3: key={}", storageKey);
            throw new RuntimeException("File not found: " + storageKey, e);
        } catch (Exception e) {
            log.error("Failed to retrieve file from S3: {}", e.getMessage());
            throw new RuntimeException("File retrieval failed", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .build()
            );
            log.debug("File deleted from S3: key={}", storageKey);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", e.getMessage());
            throw new RuntimeException("File deletion failed", e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check file existence in S3: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate a UUID-based storage key. Prevents path traversal and name collisions.
     * Format: resumes/{uuid}_{sanitizedFilename}
     */
    private String generateStorageKey(String originalFilename) {
        String sanitized = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Prevent path traversal
        sanitized = sanitized.replaceAll("[/\\\\.]", "_");
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        return "resumes/" + UUID.randomUUID() + "_" + sanitized;
    }
}
