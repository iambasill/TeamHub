package com.basilcode.emsbackend.asset.repository;

import com.basilcode.emsbackend.asset.entity.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, UUID> {
    Optional<AssetAssignment> findByAsset_IdAndReturnedAtIsNull(UUID assetId);
    List<AssetAssignment> findByEmployee_IdAndReturnedAtIsNullOrderByAssignedAtDesc(UUID employeeId);
}
