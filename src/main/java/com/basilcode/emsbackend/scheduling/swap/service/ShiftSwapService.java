package com.basilcode.emsbackend.scheduling.swap.service;

import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import com.basilcode.emsbackend.scheduling.entity.ShiftAssignment;
import com.basilcode.emsbackend.scheduling.repository.ShiftAssignmentRepository;
import com.basilcode.emsbackend.scheduling.swap.entity.ShiftSwapRequest;
import com.basilcode.emsbackend.scheduling.swap.enums.ShiftSwapStatus;
import com.basilcode.emsbackend.scheduling.swap.repository.ShiftSwapRequestRepository;
import com.basilcode.emsbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lets the employee currently on a {@link ShiftAssignment} request handing it off — to a named
 * coworker, or "anyone" (an admin resolves who at approval time). Approving actually reassigns
 * the underlying assignment's employee; nothing here creates a new assignment or touches the
 * shift/roster tables directly beyond that one field.
 */
@Service
@RequiredArgsConstructor
public class ShiftSwapService {

    private final ShiftSwapRequestRepository shiftSwapRequestRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<ShiftSwapRequest> list(ShiftSwapStatus status) {
        return status == null
                ? shiftSwapRequestRepository.findAllByOrderByCreatedAtDesc()
                : shiftSwapRequestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public ShiftSwapRequest create(UUID shiftAssignmentId, UUID proposedToEmployeeId, String reason, Employee requestingEmployee) {
        ShiftAssignment assignment = shiftAssignmentRepository.findById(shiftAssignmentId)
                .orElseThrow(() -> new NotFoundException("Shift assignment not found: " + shiftAssignmentId));
        if (!assignment.getEmployee().getId().equals(requestingEmployee.getId())) {
            throw new UnAuthorizeException("You can only request a swap for a shift assigned to you.");
        }
        if (shiftSwapRequestRepository.existsByShiftAssignment_IdAndStatus(shiftAssignmentId, ShiftSwapStatus.PENDING)) {
            throw new BadRequestException("There's already a pending swap request for this shift assignment.");
        }

        Employee proposedTo = null;
        if (proposedToEmployeeId != null) {
            proposedTo = employeeRepository.findById(proposedToEmployeeId)
                    .orElseThrow(() -> new NotFoundException("Employee not found: " + proposedToEmployeeId));
            if (proposedTo.getId().equals(requestingEmployee.getId())) {
                throw new BadRequestException("You can't propose a swap to yourself.");
            }
        }

        ShiftSwapRequest request = new ShiftSwapRequest();
        request.setShiftAssignment(assignment);
        request.setRequestedBy(requestingEmployee);
        request.setProposedTo(proposedTo);
        request.setReason(reason);
        return shiftSwapRequestRepository.save(request);
    }

    /**
     * Reassigns the shift to {@code proposedTo} if the request named someone, otherwise to
     * {@code replacementEmployeeId} (required in that case — see {@code ApproveShiftSwapRequest}).
     */
    @Transactional
    public ShiftSwapRequest approve(UUID requestId, UUID replacementEmployeeId, User decidedBy) {
        ShiftSwapRequest request = getPending(requestId);

        Employee replacement = request.getProposedTo();
        if (replacement == null) {
            if (replacementEmployeeId == null) {
                throw new BadRequestException("This is an \"anyone\" swap request — replacementEmployeeId is required to approve it.");
            }
            replacement = employeeRepository.findById(replacementEmployeeId)
                    .orElseThrow(() -> new NotFoundException("Employee not found: " + replacementEmployeeId));
        }

        ShiftAssignment assignment = request.getShiftAssignment();
        assignment.setEmployee(replacement);
        shiftAssignmentRepository.save(assignment);

        request.setStatus(ShiftSwapStatus.APPROVED);
        request.setDecidedAt(OffsetDateTime.now());
        request.setDecidedBy(decidedBy);
        return shiftSwapRequestRepository.save(request);
    }

    @Transactional
    public ShiftSwapRequest reject(UUID requestId, User decidedBy) {
        ShiftSwapRequest request = getPending(requestId);
        request.setStatus(ShiftSwapStatus.REJECTED);
        request.setDecidedAt(OffsetDateTime.now());
        request.setDecidedBy(decidedBy);
        return shiftSwapRequestRepository.save(request);
    }

    @Transactional
    public ShiftSwapRequest cancel(UUID requestId, Employee requestingEmployee) {
        ShiftSwapRequest request = getPending(requestId);
        if (!request.getRequestedBy().getId().equals(requestingEmployee.getId())) {
            throw new UnAuthorizeException("You can only cancel your own swap request.");
        }
        request.setStatus(ShiftSwapStatus.CANCELLED);
        request.setDecidedAt(OffsetDateTime.now());
        return shiftSwapRequestRepository.save(request);
    }

    private ShiftSwapRequest getPending(UUID requestId) {
        ShiftSwapRequest request = shiftSwapRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Swap request not found: " + requestId));
        if (request.getStatus() != ShiftSwapStatus.PENDING) {
            throw new BadRequestException("This swap request was already " + request.getStatus() + ".");
        }
        return request;
    }
}
