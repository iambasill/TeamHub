package com.basilcode.emsbackend.board.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Strategy contract for all file-storage backends.
 * <p>
 * Implementations are selected at startup via {@code app.storage.type} / {@code app.storage.provider}
 * in {@code application.yml}. The service layer only knows about this interface; swapping providers
 * requires zero code changes beyond config.
 */
public interface StorageProvider {

    /**
     * Persist the given file and return its publicly accessible URL together with the clean filename.
     *
     * @param file   the incoming multipart file
     * @param folder logical sub-folder/prefix (e.g. {@code "board"})
     * @return {@link StorageResult} containing the storage URL and sanitised file name
     * @throws IOException if reading the file bytes or writing to the destination fails
     */
    StorageResult store(MultipartFile file, String folder) throws IOException;

    /**
     * Retrieve the raw bytes of a previously stored file as a stream.
     * <p>
     * The {@code fileUrl} is the value originally returned by {@link #store} and persisted in the DB.
     * The caller is responsible for closing the returned stream.
     *
     * @param fileUrl the internal storage URL / key returned at upload time
     * @return an open {@link InputStream} over the file contents
     * @throws IOException if the file cannot be retrieved from the storage backend
     */
    InputStream load(String fileUrl) throws IOException;

    /**
     * Delete the previously stored file from the storage backend.
     *
     * @param fileUrl the internal storage URL / key returned at upload time
     * @throws IOException if the deletion fails
     */
    void delete(String fileUrl) throws IOException;
}
