package com.hirehub.resume.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

/**
 * Local filesystem implementation of FileStorageService.
 * For development and testing only. Production should use S3.
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.storage.base-path:./uploads/resumes}")
    private String basePath;

    private Path storageRoot;

    @PostConstruct
    public void init() {
        storageRoot = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
            log.info("File storage root: {}", storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + storageRoot, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, String contentType) {
        String uniqueKey = generateStorageKey(originalFilename);
        Path targetPath = storageRoot.resolve(uniqueKey).normalize();

        // Prevent path traversal
        if (!targetPath.startsWith(storageRoot)) {
            throw new SecurityException("Path traversal detected: " + uniqueKey);
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file: {} ({} bytes)", uniqueKey, Files.size(targetPath));
            return uniqueKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    @Override
    public InputStream retrieve(String storageKey) {
        Path filePath = storageRoot.resolve(storageKey).normalize();

        if (!filePath.startsWith(storageRoot)) {
            throw new SecurityException("Path traversal detected");
        }

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + storageKey);
        }

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path filePath = storageRoot.resolve(storageKey).normalize();

        if (!filePath.startsWith(storageRoot)) {
            throw new SecurityException("Path traversal detected");
        }

        try {
            Files.deleteIfExists(filePath);
            log.debug("Deleted file: {}", storageKey);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storageKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path filePath = storageRoot.resolve(storageKey).normalize();
        if (!filePath.startsWith(storageRoot)) {
            return false;
        }
        return Files.exists(filePath);
    }

    private String generateStorageKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uuid = UUID.randomUUID().toString();
        // Group into subdirectories for better filesystem performance
        String dir = uuid.substring(0, 2);
        return Paths.get(dir, uuid + extension).toString();
    }
}
