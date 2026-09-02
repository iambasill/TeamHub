package com.basilcode.emsbackend.board.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Saves files to the local filesystem under {@code {uploadDir}/{folder}/}.
 * Spring serves those files as static resources at {@code /uploads/**}
 * (see {@code StorageWebConfig}).
 */
public class LocalStorageProvider implements StorageProvider {

    private static final String UPLOADS_MARKER = "/uploads/";

    private final Path uploadRoot;
    private final String baseUrl;

    public LocalStorageProvider(String uploadDir, String baseUrl) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath();
        this.baseUrl = baseUrl;
    }

    @Override
    public StorageResult store(MultipartFile file, String folder) throws IOException {
        String safeName = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        Path dest = uploadRoot.resolve(folder);
        Files.createDirectories(dest);
        Files.copy(file.getInputStream(), dest.resolve(safeName), StandardCopyOption.REPLACE_EXISTING);
        String url = baseUrl + UPLOADS_MARKER + folder + "/" + safeName;
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";
        return new StorageResult(url, originalName);
    }

    /**
     * Reconstructs the filesystem path from the stored URL and opens a read stream.
     * The URL format is {@code {baseUrl}/uploads/{folder}/{filename}}.
     */
    @Override
    public InputStream load(String fileUrl) throws IOException {
        int idx = fileUrl.indexOf(UPLOADS_MARKER);
        if (idx < 0) {
            throw new IOException("Cannot resolve local file — URL does not contain '/uploads/': " + fileUrl);
        }
        String relativePath = fileUrl.substring(idx + UPLOADS_MARKER.length());
        Path filePath = uploadRoot.resolve(relativePath);
        return Files.newInputStream(filePath);
    }

    /** Strips path separators to prevent path-traversal attacks. */
    private String sanitize(String original) {
        if (original == null || original.isBlank()) return "untitled";
        return original.replaceAll("[/\\\\]", "_");
    }

    @Override
    public void delete(String fileUrl) throws IOException {
        int idx = fileUrl.indexOf(UPLOADS_MARKER);
        if (idx >= 0) {
            String relativePath = fileUrl.substring(idx + UPLOADS_MARKER.length());
            Path filePath = uploadRoot.resolve(relativePath);
            Files.deleteIfExists(filePath);
        }
    }
}
