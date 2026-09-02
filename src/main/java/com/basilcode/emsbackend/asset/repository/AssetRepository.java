package com.basilcode.emsbackend.asset.repository;

import com.basilcode.emsbackend.asset.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findAllByOrderByNameAsc();
}
