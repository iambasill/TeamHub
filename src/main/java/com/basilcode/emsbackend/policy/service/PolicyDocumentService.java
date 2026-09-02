package com.basilcode.emsbackend.policy.service;

import com.basilcode.emsbackend.board.storage.StorageProvider;
import com.basilcode.emsbackend.board.storage.StorageResult;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.policy.entity.PolicyDocument;
import com.basilcode.emsbackend.policy.repository.PolicyDocumentRepository;
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

/** Reuses the same {@link StorageProvider} board documents use — no new storage mechanism, just
 * a different logical folder ("policy" vs "board"). */
@Service
@RequiredArgsConstructor
public class PolicyDocumentService {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final StorageProvider storageProvider;

    @Transactional
    public PolicyDocument store(MultipartFile file, String title, String category, String description, User uploadedBy) throws IOException {
        StorageResult result = storageProvider.store(file, "policy");

        PolicyDocument document = new PolicyDocument();
        document.setTitle(title);
        document.setCategory(category);
        document.setDescription(description);
        document.setFileName(result.fileName());
        document.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        document.setSizeBytes(file.getSize());
        document.setFileUrl(result.fileUrl());
        document.setUploadedBy(uploadedBy);
        return policyDocumentRepository.save(document);
    }

    public List<PolicyDocument> list(String query) {
        if (query == null || query.isBlank()) {
            return policyDocumentRepository.findAllByOrderByCategoryAscTitleAsc();
        }
        return policyDocumentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCategoryAscTitleAsc(query, query);
    }

    public PolicyDocument get(UUID id) {
        return policyDocumentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Policy document not found: " + id));
    }

    public FileDownload download(UUID id) throws IOException {
        PolicyDocument document = get(id);
        if (document.getFileUrl() == null) {
            throw new NotFoundException("This document has no file attached.");
        }
        InputStream content = storageProvider.load(document.getFileUrl());
        return new FileDownload(document, content);
    }

    @Transactional
    public void delete(UUID id, User user) {
        PolicyDocument document = get(id);
        boolean isAdmin = user.getUserType() == UserTypeEnum.ADMIN || user.getUserType() == UserTypeEnum.SUPER_ADMIN;
        boolean isUploader = document.getUploadedBy().getId().equals(user.getId());
        if (!isAdmin && !isUploader) {
            throw new UnAuthorizeException("You are not authorized to delete this document.");
        }

        if (document.getFileUrl() != null) {
            try {
                storageProvider.delete(document.getFileUrl());
            } catch (IOException e) {
                // Same tolerant behavior as BoardDocumentService — if it's already gone from
                // storage, we still want the DB record removed.
                System.err.println("Failed to delete policy document file from storage: " + e.getMessage());
            }
        }
        policyDocumentRepository.delete(document);
    }

    public record FileDownload(PolicyDocument document, InputStream content) {
    }
}
