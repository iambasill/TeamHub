package com.basilcode.emsbackend.booking;

import com.basilcode.emsbackend.booking.dto.BookingDtoMapper;
import com.basilcode.emsbackend.booking.dto.CreateBookingRequest;
import com.basilcode.emsbackend.booking.dto.CreateResourceRequest;
import com.basilcode.emsbackend.booking.dto.ResourceBookingDto;
import com.basilcode.emsbackend.booking.dto.ResourceDto;
import com.basilcode.emsbackend.booking.entity.Resource;
import com.basilcode.emsbackend.booking.entity.ResourceBooking;
import com.basilcode.emsbackend.booking.service.BookingService;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Booking a resource is open to any employee; adding new bookable resources is admin-managed.
 * Cancelling is allowed for the original booker or HR/ADMIN/SUPER_ADMIN/MANAGER — checked inline
 * since it's a two-way OR, not a single role set. */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingDtoMapper mapper;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<ResourceDto>>> listResources() {
        List<ResourceDto> resources = bookingService.listResources().stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved", resources));
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/resources")
    public ResponseEntity<ApiResponse<ResourceDto>> createResource(@Valid @RequestBody CreateResourceRequest request) {
        Resource resource = bookingService.createResource(request.name(), request.description(), request.capacity());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Resource added", mapper.toDto(resource)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceBookingDto>>> list(
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) OffsetDateTime start,
            @RequestParam(required = false) OffsetDateTime end) {
        List<ResourceBookingDto> bookings = bookingService.search(resourceId, start, end).stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", bookings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResourceBookingDto>> create(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        ResourceBooking booking = bookingService.createBooking(
                request.resourceId(), request.title(), request.startsAt(), request.endsAt(), employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Resource booked", mapper.toDto(booking)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = resolveEmployee(currentUser);
        boolean isManagement = currentUser.getAuthorities().stream()
                .anyMatch(a -> List.of("ROLE_MANAGER", "ROLE_HR", "ROLE_ADMIN", "ROLE_SUPER_ADMIN").contains(a.getAuthority()));
        bookingService.cancelBooking(id, employee, isManagement);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled"));
    }

    private Employee resolveEmployee(UserDetails currentUser) {
        return employeeRepository.findByUser_EmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("Employee record not found for current user"));
    }
}
