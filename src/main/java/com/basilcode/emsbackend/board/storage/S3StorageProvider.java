package com.basilcode.emsbackend.board.storage;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Stores files in an AWS S3 bucket.
 * Credentials are resolved by the AWS SDK default chain:
 * {@code AWS_ACCESS_KEY_ID} / {@code AWS_SECRET_ACCESS_KEY} env vars → instance profile → etc.
 */
public class S3StorageProvider implements StorageProvider {

    private final S3Client s3;
    private final String bucket;
    private final String region;

    public S3StorageProvider(String bucket, String region) {
        this.bucket = bucket;
        this.region = region;
        this.s3 = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public StorageResult store(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";
        String key = folder + "/" + UUID.randomUUID() + "_" + originalName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .build();

        s3.putObject(request, RequestBody.fromBytes(file.getBytes()));

        // Stored as internal reference — never exposed to the client
        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        return new StorageResult(url, originalName);
    }

    /** Parses the S3 object key from the stored URL and streams the object. */
    @Override
    public InputStream load(String fileUrl) throws IOException {
        String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        String key = fileUrl.substring(prefix.length());
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /** Parses the S3 object key from the stored URL and deletes the object. */
    @Override
    public void delete(String fileUrl) throws IOException {
        String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        String key = fileUrl.substring(prefix.length());
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
