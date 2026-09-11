package com.basilcode.emsbackend.scheduling;

import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.scheduling.dto.CreateShiftAssignmentRequest;
import com.basilcode.emsbackend.scheduling.dto.CreateShiftRequest;
import com.basilcode.emsbackend.scheduling.dto.SchedulingDtoMapper;
import com.basilcode.emsbackend.scheduling.dto.ShiftAssignmentDto;
import com.basilcode.emsbackend.scheduling.dto.ShiftDto;
import com.basilcode.emsbackend.scheduling.entity.Shift;
import com.basilcode.emsbackend.scheduling.entity.ShiftAssignment;
import com.basilcode.emsbackend.scheduling.service.SchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Shift definitions + roster, open for every employee to view (so "who's on duty" is genuinely
 * useful as a transparency feature) but only ops-managing roles can define shifts or roster
 * people onto them — same role set used for leave approval elsewhere in the app.
 */
@RestController
@RequestMapping("/scheduling")
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;
    private final SchedulingDtoMapper mapper;

    @GetMapping("/shifts")
    public ResponseEntity<ApiResponse<List<ShiftDto>>> listShifts() {
        List<ShiftDto> shifts = schedulingService.listShifts().stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Shifts retrieved", shifts));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/shifts")
    public ResponseEntity<ApiResponse<ShiftDto>> createShift(@Valid @RequestBody CreateShiftRequest request) {
        Shift shift = schedulingService.createShift(request.name(), request.startTime(), request.endTime(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Shift created", mapper.toDto(shift)));
    }

    @GetMapping("/roster")
    public ResponseEntity<ApiResponse<List<ShiftAssignmentDto>>> getRoster(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        List<ShiftAssignmentDto> roster = schedulingService.listRoster(start, end).stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Roster retrieved", roster));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/roster")
    public ResponseEntity<ApiResponse<ShiftAssignmentDto>> assign(@Valid @RequestBody CreateShiftAssignmentRequest request) {
        ShiftAssignment assignment = schedulingService.assign(request.shiftId(), request.employeeId(), request.date());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Employee rostered onto shift", mapper.toDto(assignment)));
    }

    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/roster/{id}")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(@PathVariable UUID id) {
        schedulingService.removeAssignment(id);
        return ResponseEntity.ok(ApiResponse.success("Roster assignment removed"));
    }

    @GetMapping("/on-duty")
    public ResponseEntity<ApiResponse<List<ShiftAssignmentDto>>> currentlyOnDuty() {
        List<ShiftAssignmentDto> onDuty = schedulingService.currentlyOnDuty().stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Currently on-duty roster retrieved", onDuty));
    }
}
