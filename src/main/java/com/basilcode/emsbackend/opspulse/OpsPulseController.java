package com.basilcode.emsbackend.opspulse;

import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.opspulse.dto.OpsPulseDto;
import com.basilcode.emsbackend.opspulse.service.OpsPulseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only overview, open to any authenticated employee — no special role gate, same as
 * {@code NotificationController}. */
@RestController
@RequestMapping("/ops-pulse")
@RequiredArgsConstructor
public class OpsPulseController {

    private final OpsPulseService opsPulseService;

    @GetMapping
    public ResponseEntity<ApiResponse<OpsPulseDto>> getPulse() {
        return ResponseEntity.ok(ApiResponse.success("Ops pulse computed", opsPulseService.buildPulse()));
    }
}
