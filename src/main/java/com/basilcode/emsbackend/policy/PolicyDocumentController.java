package com.basilcode.emsbackend.policy;

import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.policy.dto.PolicyDocumentDto;
import com.basilcode.emsbackend.policy.entity.PolicyDocument;
import com.basilcode.emsbackend.policy.service.PolicyDocumentService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

/** Open for every authenticated employee to browse/download; uploading and deleting are
 * uploader-or-admin, same discipline as Board documents (see {@code BoardDocumentController}). */
@RestController
@RequestMapping("/policy-documents")
@RequiredArgsConstructor
public class PolicyDocumentController {

    private final PolicyDocumentService policyDocumentService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyDocumentDto>>> list(@RequestParam(required = false) String q) {
        List<PolicyDocumentDto> documents = policyDocumentService.list(q).stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Policy documents retrieved", documents));
    }

    // @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping
    public ResponseEntity<ApiResponse<PolicyDocumentDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails currentUser) {
        User uploadedBy = resolveUser(currentUser);
        try {
            PolicyDocument document = policyDocumentService.store(file, title, category, description, uploadedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Policy document uploaded", toDto(document)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        policyDocumentService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.success("Policy document deleted", null));
    }

    /** Streams the raw file bytes back — the internal fileUrl is never exposed. */
    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id) {
        try {
            PolicyDocumentService.FileDownload dl = policyDocumentService.download(id);
            PolicyDocument doc = dl.document();

            StreamingResponseBody body = outputStream -> {
                try (var content = dl.content()) {
                    content.transferTo(outputStream);
                }
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(doc.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(doc.getSizeBytes()))
                    .body(body);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to stream file from storage", e);
        }
    }

    private User resolveUser(UserDetails currentUser) {
        return userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
    }

    private PolicyDocumentDto toDto(PolicyDocument document) {
        User uploadedBy = document.getUploadedBy();
        return new PolicyDocumentDto(
                document.getId(), document.getTitle(), document.getCategory(), document.getDescription(),
                document.getFileName(), document.getContentType(), document.getSizeBytes(),
                uploadedBy.getFirstName() + " " + uploadedBy.getLastName(), document.getUploadedAt());
    }
}
