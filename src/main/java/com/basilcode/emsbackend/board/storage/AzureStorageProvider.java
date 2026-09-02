package com.basilcode.emsbackend.board.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Stores files in Azure Blob Storage.
 * Requires {@code AZURE_STORAGE_CONNECTION_STRING} env var and
 * {@code app.storage.azure.container} config property.
 */
public class AzureStorageProvider implements StorageProvider {

    private final BlobContainerClient containerClient;

    public AzureStorageProvider(String container, String connectionString) {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = serviceClient.getBlobContainerClient(container);
        if (!containerClient.exists()) {
            containerClient.create();
        }
    }

    @Override
    public StorageResult store(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";
        String blobName = folder + "/" + UUID.randomUUID() + "_" + originalName;

        containerClient.getBlobClient(blobName)
                .upload(file.getInputStream(), file.getSize(), true);

        // Internal reference — not exposed to the client
        String url = containerClient.getBlobContainerUrl() + "/" + blobName;
        return new StorageResult(url, originalName);
    }

    /** Parses the blob name from the stored URL and streams it from Azure. */
    @Override
    public InputStream load(String fileUrl) throws IOException {
        String containerUrl = containerClient.getBlobContainerUrl();
        String blobName = fileUrl.substring(containerUrl.length() + 1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        containerClient.getBlobClient(blobName).download(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    /** Parses the blob name from the stored URL and deletes it from Azure. */
    @Override
    public void delete(String fileUrl) throws IOException {
        String containerUrl = containerClient.getBlobContainerUrl();
        String blobName = fileUrl.substring(containerUrl.length() + 1);
        containerClient.getBlobClient(blobName).deleteIfExists();
    }
}
