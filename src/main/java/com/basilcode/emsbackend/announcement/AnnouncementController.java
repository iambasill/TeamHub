package com.basilcode.emsbackend.announcement;

import com.basilcode.emsbackend.announcement.dto.AnnouncementDto;
import com.basilcode.emsbackend.announcement.dto.CreateAnnouncementRequest;
import com.basilcode.emsbackend.announcement.entity.Announcement;
import com.basilcode.emsbackend.announcement.service.AnnouncementService;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Open for every authenticated employee to read; only HR/ADMIN/SUPER_ADMIN can post or remove one. */
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserRepository userRepository;

    /** Only announcements the current user is actually a target of — see
     * {@code AnnouncementService#listActiveFor}. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> list(@AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        List<AnnouncementDto> announcements = announcementService.listActiveFor(user).stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved", announcements));
    }

    // @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementDto>> create(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        Announcement announcement = announcementService.create(
                request.title(), request.body(), request.pinned(), request.expiresAt(),
                request.targetAudiences(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Announcement posted", toDto(announcement)));
    }

    // @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Announcement removed"));
    }

    private User resolveUser(UserDetails currentUser) {
        return userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
    }

    private AnnouncementDto toDto(Announcement a) {
        User creator = a.getCreatedBy();
        List<String> audiences = a.getTargetAudiences().stream().map(Enum::name).toList();
        return new AnnouncementDto(
                a.getId(), a.getTitle(), a.getBody(), a.isPinned(), audiences,
                creator.getFirstName() + " " + creator.getLastName(), a.getCreatedAt(), a.getExpiresAt());
    }
}
