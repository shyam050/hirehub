package com.hirehub.resume.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Abstraction for file storage operations.
 * Local implementation for development; S3 implementation for production.
 */
public interface FileStorageService {

    /**
     * Store a file and return a unique storage key.
     * The key should be unusable for path traversal and safe for any storage backend.
     */
    String store(InputStream inputStream, String originalFilename, String contentType);

    /**
     * Retrieve a file as an InputStream by its storage key.
     */
    InputStream retrieve(String storageKey);

    /**
     * Delete a file by its storage key.
     */
    void delete(String storageKey);

    /**
     * Check if a file exists for the given storage key.
     */
    boolean exists(String storageKey);
}
