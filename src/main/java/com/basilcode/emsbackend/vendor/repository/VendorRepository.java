package com.basilcode.emsbackend.vendor.repository;

import com.basilcode.emsbackend.vendor.entity.Vendor;
import com.basilcode.emsbackend.vendor.enums.VendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findAllByOrderByNameAsc();
    List<Vendor> findByStatusOrderByNameAsc(VendorStatus status);
}
