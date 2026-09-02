package com.basilcode.emsbackend.board.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Stores files in Google Cloud Storage.
 * Credentials are resolved via Application Default Credentials
 * (set {@code GOOGLE_APPLICATION_CREDENTIALS} env var to a service-account JSON path).
 */
public class GcsStorageProvider implements StorageProvider {

    private final Storage gcs;
    private final String bucket;

    public GcsStorageProvider(String bucket) {
        this.bucket = bucket;
        this.gcs = StorageOptions.getDefaultInstance().getService();
    }

    @Override
    public StorageResult store(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";
        String blobName = folder + "/" + UUID.randomUUID() + "_" + originalName;
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        BlobId blobId = BlobId.of(bucket, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        gcs.create(blobInfo, file.getBytes());

        // Internal reference — not exposed to the client
        String url = "https://storage.googleapis.com/" + bucket + "/" + blobName;
        return new StorageResult(url, originalName);
    }

    /** Parses the blob name from the stored URL and downloads the object from GCS. */
    @Override
    public InputStream load(String fileUrl) throws IOException {
        String prefix = "https://storage.googleapis.com/" + bucket + "/";
        String blobName = fileUrl.substring(prefix.length());
        Blob blob = gcs.get(BlobId.of(bucket, blobName));
        if (blob == null) {
            throw new IOException("GCS blob not found: " + blobName);
        }
        return new ByteArrayInputStream(blob.getContent());
    }

    /** Parses the blob name from the stored URL and deletes it from GCS. */
    @Override
    public void delete(String fileUrl) throws IOException {
        String prefix = "https://storage.googleapis.com/" + bucket + "/";
        String blobName = fileUrl.substring(prefix.length());
        gcs.delete(BlobId.of(bucket, blobName));
    }
}
