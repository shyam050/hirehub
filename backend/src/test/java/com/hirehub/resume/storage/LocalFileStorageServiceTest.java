package com.hirehub.resume.storage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalFileStorageService — no Spring context needed.
 */
class LocalFileStorageServiceTest {

    private LocalFileStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService();
        // Use reflection to set fields before @PostConstruct
        org.springframework.test.util.ReflectionTestUtils.setField(storageService, "basePath", tempDir.toString());
        // Manually initialize (skip @PostConstruct)
        try {
            var initMethod = LocalFileStorageService.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(storageService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Store and retrieve a file")
    void storeAndRetrieve() {
        byte[] content = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        InputStream is = new ByteArrayInputStream(content);

        String key = storageService.store(is, "test.pdf", "application/pdf");

        assertNotNull(key);
        assertTrue(key.endsWith(".pdf"), "Key should end with .pdf extension");

        // Retrieve
        InputStream retrieved = storageService.retrieve(key);
        assertNotNull(retrieved);

        try {
            byte[] retrievedBytes = retrieved.readAllBytes();
            assertArrayEquals(content, retrievedBytes);
        } catch (Exception e) {
            fail("Failed to read retrieved file", e);
        }
    }

    @Test
    @DisplayName("Delete removes file")
    void deleteRemovesFile() {
        byte[] content = "Delete me".getBytes(StandardCharsets.UTF_8);
        String key = storageService.store(new ByteArrayInputStream(content), "delete.pdf", "application/pdf");

        assertTrue(storageService.exists(key));
        storageService.delete(key);
        assertFalse(storageService.exists(key));
    }

    @Test
    @DisplayName("Storage key prevents path traversal")
    void storageKeyPreventsPathTraversal() {
        String key = storageService.store(
                new ByteArrayInputStream("test".getBytes()),
                "../../etc/passwd.pdf",
                "application/pdf"
        );

        assertFalse(key.contains(".."), "Key should not contain path traversal");
        assertFalse(key.contains("/etc"), "Key should not reference system paths");
    }

    @Test
    @DisplayName("Duplicate filenames produce unique keys")
    void duplicateFilenamesProduceUniqueKeys() {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        String key1 = storageService.store(new ByteArrayInputStream(content), "same.pdf", "application/pdf");
        String key2 = storageService.store(new ByteArrayInputStream(content), "same.pdf", "application/pdf");

        assertNotEquals(key1, key2, "Storage keys should be unique even for same filename");
    }

    @Test
    @DisplayName("Retrieve nonexistent file throws exception")
    void retrieveNonexistentThrows() {
        assertThrows(RuntimeException.class,
                () -> storageService.retrieve("resumes/nonexistent-key"));
    }
}
