package com.basilcode.emsbackend.vendor;

import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.vendor.dto.CreateVendorRequest;
import com.basilcode.emsbackend.vendor.dto.UpdateVendorRequest;
import com.basilcode.emsbackend.vendor.dto.VendorDto;
import com.basilcode.emsbackend.vendor.entity.Vendor;
import com.basilcode.emsbackend.vendor.enums.VendorStatus;
import com.basilcode.emsbackend.vendor.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** A back-office directory — every endpoint is HR/ADMIN/SUPER_ADMIN only, unlike Assets/Incidents
 * which have an open read tier for all employees. */
@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')")
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VendorDto>>> list(@RequestParam(required = false) String status) {
        VendorStatus statusEnum = status == null ? null : parseStatus(status);
        List<VendorDto> vendors = vendorService.list(statusEnum).stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved", vendors));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VendorDto>> create(@Valid @RequestBody CreateVendorRequest request) {
        Vendor vendor = vendorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Vendor added", toDto(vendor)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VendorDto>> update(@PathVariable UUID id, @RequestBody UpdateVendorRequest request) {
        Vendor vendor = vendorService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Vendor updated", toDto(vendor)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<VendorDto>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor activated", toDto(vendorService.setStatus(id, VendorStatus.ACTIVE))));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<VendorDto>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor deactivated", toDto(vendorService.setStatus(id, VendorStatus.INACTIVE))));
    }

    private VendorStatus parseStatus(String value) {
        try {
            return VendorStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + value);
        }
    }

    private VendorDto toDto(Vendor v) {
        return new VendorDto(
                v.getId(), v.getName(), v.getCategory(), v.getContactName(), v.getContactEmail(),
                v.getContactPhone(), v.getNotes(), v.getStatus().name(), v.getCreatedAt(), v.getUpdatedAt());
    }
}
