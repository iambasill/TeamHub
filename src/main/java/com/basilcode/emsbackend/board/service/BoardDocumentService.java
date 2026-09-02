package com.basilcode.emsbackend.board.service;

import com.basilcode.emsbackend.board.entity.BoardDocument;
import com.basilcode.emsbackend.board.repository.BoardDocumentRepository;
import com.basilcode.emsbackend.board.storage.StorageProvider;
import com.basilcode.emsbackend.board.storage.StorageResult;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardDocumentService {

    private final BoardDocumentRepository boardDocumentRepository;
    private final StorageProvider storageProvider;

    @Transactional
    public BoardDocument store(MultipartFile file, User uploadedBy, boolean isRecording) throws IOException {
        StorageResult result = storageProvider.store(file, "board");

        BoardDocument document = new BoardDocument();
        document.setFileName(result.fileName());
        document.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        document.setSizeBytes(file.getSize());
        document.setFileUrl(result.fileUrl());
        document.setUploadedBy(uploadedBy);
        document.setRecording(isRecording);

        return boardDocumentRepository.save(document);
    }

    public List<BoardDocument> list() {
        return boardDocumentRepository.findAllByOrderByUploadedAtDesc();
    }

    public BoardDocument get(UUID id) {
        return boardDocumentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    /**
     * Fetches document metadata and opens a read stream from the active storage backend.
     * <p>
     * The {@code fileUrl} is an internal detail — it is never returned to the client.
     * The caller is responsible for closing the returned stream.
     *
     * @return a bundle of the document entity (for metadata) and its content stream
     */
    /**
     * Delete a document.
     * Allowed if the user is an admin OR if the user is the uploader and it was uploaded within the last 30 minutes.
     */
    @Transactional
    public void delete(UUID id, User user) {
        BoardDocument document = get(id);
        
        boolean isAdmin = user.getUserType() == UserTypeEnum.ADMIN ||
                          user.getUserType() == UserTypeEnum.SUPER_ADMIN;
        boolean isUploader = document.getUploadedBy().getId().equals(user.getId());

        if (!isAdmin && !isUploader) {
            throw new UnAuthorizeException("You are not authorized to delete this document.");
        }

        if (isUploader && !isAdmin) {
            long minutesSinceUpload = java.time.Duration.between(document.getUploadedAt(), java.time.OffsetDateTime.now()).toMinutes();
            if (minutesSinceUpload > 30) {
                throw new UnAuthorizeException("You can only delete documents within 30 minutes of uploading.");
            }
        }
        
        if (document.getFileUrl() != null) {
            try {
                storageProvider.delete(document.getFileUrl());
            } catch (IOException e) {
                // Log and swallow, or throw. If the file is already gone from cloud, we still want to delete DB record.
                System.err.println("Failed to delete file from storage: " + e.getMessage());
            }
        }
        
        boardDocumentRepository.delete(document);
    }

    /**
     * Resolves the storage URL from the database and returns an open stream ready for download.
     */
    public FileDownload download(UUID id) throws IOException {
        BoardDocument document = get(id);
        if (document.getFileUrl() == null) {
            throw new NotFoundException("This document has no file attached (legacy record).");
        }
        InputStream content = storageProvider.load(document.getFileUrl());
        return new FileDownload(document, content);
    }

    /** Bundles document metadata with its content stream for the download endpoint. */
    public record FileDownload(BoardDocument document, InputStream content) {}
}
