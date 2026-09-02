package com.basilcode.emsbackend.asset.service;

import com.basilcode.emsbackend.asset.entity.Asset;
import com.basilcode.emsbackend.asset.entity.AssetAssignment;
import com.basilcode.emsbackend.asset.enums.AssetStatus;
import com.basilcode.emsbackend.asset.repository.AssetAssignmentRepository;
import com.basilcode.emsbackend.asset.repository.AssetRepository;
import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public Asset create(String name, String category, String serialNumber, String notes) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setCategory(category);
        asset.setSerialNumber(serialNumber);
        asset.setNotes(notes);
        return assetRepository.save(asset);
    }

    public List<Asset> list() {
        return assetRepository.findAllByOrderByNameAsc();
    }

    public Optional<AssetAssignment> currentAssignment(UUID assetId) {
        return assetAssignmentRepository.findByAsset_IdAndReturnedAtIsNull(assetId);
    }

    public List<AssetAssignment> myAssets(UUID employeeId) {
        return assetAssignmentRepository.findByEmployee_IdAndReturnedAtIsNullOrderByAssignedAtDesc(employeeId);
    }

    @Transactional
    public AssetAssignment assign(UUID assetId, UUID employeeId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + assetId));
        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new BadRequestException("Asset is not available (current status: " + asset.getStatus() + ").");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found: " + employeeId));

        AssetAssignment assignment = new AssetAssignment();
        assignment.setAsset(asset);
        assignment.setEmployee(employee);
        assignment = assetAssignmentRepository.save(assignment);

        asset.setStatus(AssetStatus.ASSIGNED);
        assetRepository.save(asset);
        return assignment;
    }

    @Transactional
    public Asset returnAsset(UUID assetId, String conditionNotes) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found: " + assetId));
        AssetAssignment assignment = assetAssignmentRepository.findByAsset_IdAndReturnedAtIsNull(assetId)
                .orElseThrow(() -> new BadRequestException("This asset isn't currently assigned to anyone."));

        assignment.setReturnedAt(OffsetDateTime.now());
        assignment.setConditionNotes(conditionNotes);
        assetAssignmentRepository.save(assignment);

        asset.setStatus(AssetStatus.AVAILABLE);
        return assetRepository.save(asset);
    }
}
