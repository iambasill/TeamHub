package com.basilcode.emsbackend.asset;

import com.basilcode.emsbackend.asset.dto.AssetDto;
import com.basilcode.emsbackend.asset.dto.AssignAssetRequest;
import com.basilcode.emsbackend.asset.dto.CreateAssetRequest;
import com.basilcode.emsbackend.asset.dto.ReturnAssetRequest;
import com.basilcode.emsbackend.asset.entity.Asset;
import com.basilcode.emsbackend.asset.entity.AssetAssignment;
import com.basilcode.emsbackend.asset.service.AssetService;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.user.entity.User;
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

/**
 * Full inventory is management-only (HR/ADMIN/SUPER_ADMIN) — most employees have no reason to see
 * every laptop the company owns, only what's checked out to them, which is {@code /assets/mine}.
 */
@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final EmployeeRepository employeeRepository;

    // @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetDto>>> list() {
        List<AssetDto> assets = assetService.list().stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Assets retrieved", assets));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<AssetDto>>> myAssets(@AuthenticationPrincipal UserDetails currentUser) {
        Employee employee = employeeRepository.findByUser_EmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("Employee record not found for current user"));
        List<AssetDto> assets = assetService.myAssets(employee.getId()).stream()
                .map(assignment -> toDto(assignment.getAsset(), assignment))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Your assets retrieved", assets));
    }

    // @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping
    public ResponseEntity<ApiResponse<AssetDto>> create(@Valid @RequestBody CreateAssetRequest request) {
        Asset asset = assetService.create(request.name(), request.category(), request.serialNumber(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Asset added", toDto(asset)));
    }

    // @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<AssetDto>> assign(@PathVariable UUID id, @Valid @RequestBody AssignAssetRequest request) {
        AssetAssignment assignment = assetService.assign(id, request.employeeId());
        return ResponseEntity.ok(ApiResponse.success("Asset assigned", toDto(assignment.getAsset(), assignment)));
    }

    // @PreAuthorize("hasAnyRole('HR','ADMIN','SUPER_ADMIN')") // TODO: re-enable when permission model is finalised
    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse<AssetDto>> returnAsset(
            @PathVariable UUID id, @RequestBody(required = false) ReturnAssetRequest request) {
        String notes = request != null ? request.conditionNotes() : null;
        Asset asset = assetService.returnAsset(id, notes);
        return ResponseEntity.ok(ApiResponse.success("Asset returned", toDto(asset)));
    }

    private AssetDto toDto(Asset asset) {
        return toDto(asset, assetService.currentAssignment(asset.getId()).orElse(null));
    }

    private AssetDto toDto(Asset asset, AssetAssignment assignment) {
        Employee holder = assignment != null ? assignment.getEmployee() : null;
        User holderUser = holder != null ? holder.getUser() : null;
        return new AssetDto(
                asset.getId(), asset.getName(), asset.getCategory(), asset.getSerialNumber(),
                asset.getStatus().name(), asset.getNotes(),
                holder != null ? holder.getId() : null,
                holderUser != null ? holderUser.getFirstName() + " " + holderUser.getLastName() : null,
                assignment != null ? assignment.getAssignedAt() : null);
    }
}
