package com.basilcode.emsbackend.board.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reads {@code app.storage.type} (local | cloud) and, for cloud,
 * {@code app.storage.provider} (s3 | cloudinary | gcs | azure) from config,
 * then instantiates exactly one {@link StorageProvider} bean.
 *
 * <p>All cloud-specific {@code @Value} fields default to an empty string so the
 * app can start with {@code type=local} without needing any cloud credentials.
 */
@Configuration
public class StorageConfig {

    @Value("${app.storage.type:local}")
    private String type;

    @Value("${app.storage.provider:}")
    private String provider;

    // Local disk
    @Value("${app.storage.local.upload-dir:uploads}")
    private String localUploadDir;

    @Value("${app.storage.local.base-url:http://localhost:8080/api/v1}")
    private String localBaseUrl;

    // AWS S3
    @Value("${app.storage.s3.bucket:}")
    private String s3Bucket;

    @Value("${app.storage.s3.region:}")
    private String s3Region;

    // Cloudinary
    @Value("${app.storage.cloudinary.cloud-name:}")
    private String cloudinaryCloudName;

    @Value("${app.storage.cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${app.storage.cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    // Google Cloud Storage
    @Value("${app.storage.gcs.bucket:}")
    private String gcsBucket;

    // Azure Blob Storage
    @Value("${app.storage.azure.container:}")
    private String azureContainer;

    @Value("${app.storage.azure.connection-string:}")
    private String azureConnectionString;

    @Bean
    public StorageProvider storageProvider() {
        return switch (type.trim().toLowerCase()) {
            case "local" -> new LocalStorageProvider(localUploadDir, localBaseUrl);
            case "cloud" -> buildCloudProvider();
            default -> throw new IllegalStateException(
                    "Unknown app.storage.type='" + type + "'. Allowed values: local, cloud");
        };
    }

    private StorageProvider buildCloudProvider() {
        return switch (provider.trim().toLowerCase()) {
            case "s3" -> {
                requireNonBlank(s3Bucket, "app.storage.s3.bucket");
                requireNonBlank(s3Region, "app.storage.s3.region");
                yield new S3StorageProvider(s3Bucket, s3Region);
            }
            case "cloudinary" -> {
                requireNonBlank(cloudinaryCloudName, "app.storage.cloudinary.cloud-name");
                requireNonBlank(cloudinaryApiKey, "app.storage.cloudinary.api-key");
                requireNonBlank(cloudinaryApiSecret, "app.storage.cloudinary.api-secret");
                yield new CloudinaryStorageProvider(cloudinaryCloudName, cloudinaryApiKey, cloudinaryApiSecret);
            }
            case "gcs" -> {
                requireNonBlank(gcsBucket, "app.storage.gcs.bucket");
                yield new GcsStorageProvider(gcsBucket);
            }
            case "azure" -> {
                requireNonBlank(azureContainer, "app.storage.azure.container");
                requireNonBlank(azureConnectionString, "app.storage.azure.connection-string");
                yield new AzureStorageProvider(azureContainer, azureConnectionString);
            }
            default -> throw new IllegalStateException(
                    "Unknown app.storage.provider='" + provider + "'. Allowed values: s3, cloudinary, gcs, azure");
        };
    }

    private void requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "app.storage.type=cloud requires '" + key + "' to be set in config / .env");
        }
    }
}
