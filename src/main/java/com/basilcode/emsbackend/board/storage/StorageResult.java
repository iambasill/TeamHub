package com.basilcode.emsbackend.board.storage;

/**
 * Value object returned by every {@link StorageProvider} implementation.
 *
 * @param fileUrl  The publicly accessible URL for the stored file.
 * @param fileName The original (sanitised) file name.
 */
public record StorageResult(String fileUrl, String fileName) {}
