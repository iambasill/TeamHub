package com.basilcode.emsbackend.board;

import com.basilcode.emsbackend.board.dto.BoardDocumentDto;
import com.basilcode.emsbackend.board.entity.BoardDocument;
import com.basilcode.emsbackend.board.service.BoardDocumentService;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
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

@RestController
@RequestMapping("/board/documents")
@RequiredArgsConstructor
@PreAuthorize("@boardMembershipService.isCurrentUserMember(authentication)")
public class BoardDocumentController {

    private final BoardDocumentService boardDocumentService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<BoardDocumentDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "isRecording", defaultValue = "false") boolean isRecording,
            @AuthenticationPrincipal UserDetails currentUser) {
        User uploadedBy = userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
        try {
            BoardDocument document = boardDocumentService.store(file, uploadedBy, isRecording);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document uploaded", toDto(document)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardDocumentDto>>> list() {
        List<BoardDocumentDto> documents = boardDocumentService.list().stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved", documents));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
        boardDocumentService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }

    /**
     * Streams the raw file bytes back to the client via the active storage backend.
     * The internal {@code fileUrl} is never exposed — the backend acts as a transparent proxy.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id) {
        try {
            BoardDocumentService.FileDownload dl = boardDocumentService.download(id);
            BoardDocument doc = dl.document();

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

    private BoardDocumentDto toDto(BoardDocument document) {
        User uploadedBy = document.getUploadedBy();
        return new BoardDocumentDto(
                document.getId(),
                document.getFileName(),
                document.getContentType(),
                document.getSizeBytes(),
                uploadedBy.getFirstName() + " " + uploadedBy.getLastName(),
                document.getUploadedAt(),
                document.isRecording()
        );
    }
}
