package com.basilcode.emsbackend.board.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Stores files on Cloudinary.
 * Credentials ({@code CLOUDINARY_API_KEY}, {@code CLOUDINARY_API_SECRET}) and
 * {@code CLOUDINARY_CLOUD_NAME} are read from env via {@code application.yml}.
 */
public class CloudinaryStorageProvider implements StorageProvider {

    private final Cloudinary cloudinary;

    public CloudinaryStorageProvider(String cloudName, String apiKey, String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StorageResult store(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";
        String publicId = folder + "/" + UUID.randomUUID();

        Map<String, Object> options = ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", "auto",
                "use_filename", false
        );

        Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), options);
        String url = (String) result.get("secure_url");
        return new StorageResult(url, originalName);
    }

    /**
     * Fetches the file from Cloudinary's CDN URL.
     * The secure_url is publicly accessible over HTTPS, so a plain HTTP GET retrieves the bytes.
     */
    @Override
    public InputStream load(String fileUrl) throws IOException {
        return URI.create(fileUrl).toURL().openStream();
    }

    @Override
    public void delete(String fileUrl) throws IOException {
        try {
            // URL format typically: https://res.cloudinary.com/.../image/upload/v1234567890/folder/uuid.ext
            // We need to extract the "folder/uuid" part as the public_id.
            int uploadIdx = fileUrl.indexOf("/upload/");
            if (uploadIdx >= 0) {
                String path = fileUrl.substring(uploadIdx + 8);
                // Remove version tag if present (e.g. v1234567890/)
                if (path.matches("^v\\d+/.*")) {
                    path = path.replaceFirst("^v\\d+/", "");
                }
                // Remove extension
                int dotIdx = path.lastIndexOf('.');
                if (dotIdx > 0) {
                    path = path.substring(0, dotIdx);
                }
                cloudinary.uploader().destroy(path, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            throw new IOException("Failed to delete file from Cloudinary: " + fileUrl, e);
        }
    }
}
