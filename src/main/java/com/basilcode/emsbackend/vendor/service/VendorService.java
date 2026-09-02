package com.basilcode.emsbackend.vendor.service;

import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.vendor.dto.CreateVendorRequest;
import com.basilcode.emsbackend.vendor.dto.UpdateVendorRequest;
import com.basilcode.emsbackend.vendor.entity.Vendor;
import com.basilcode.emsbackend.vendor.enums.VendorStatus;
import com.basilcode.emsbackend.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    @Transactional
    public Vendor create(CreateVendorRequest request) {
        Vendor vendor = new Vendor();
        vendor.setName(request.name());
        vendor.setCategory(request.category());
        vendor.setContactName(request.contactName());
        vendor.setContactEmail(request.contactEmail());
        vendor.setContactPhone(request.contactPhone());
        vendor.setNotes(request.notes());
        return vendorRepository.save(vendor);
    }

    public List<Vendor> list(VendorStatus status) {
        return status == null ? vendorRepository.findAllByOrderByNameAsc() : vendorRepository.findByStatusOrderByNameAsc(status);
    }

    @Transactional
    public Vendor update(UUID id, UpdateVendorRequest request) {
        Vendor vendor = get(id);
        if (request.name() != null) vendor.setName(request.name());
        if (request.category() != null) vendor.setCategory(request.category());
        if (request.contactName() != null) vendor.setContactName(request.contactName());
        if (request.contactEmail() != null) vendor.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) vendor.setContactPhone(request.contactPhone());
        if (request.notes() != null) vendor.setNotes(request.notes());
        return vendorRepository.save(vendor);
    }

    @Transactional
    public Vendor setStatus(UUID id, VendorStatus status) {
        Vendor vendor = get(id);
        vendor.setStatus(status);
        return vendorRepository.save(vendor);
    }

    private Vendor get(UUID id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendor not found: " + id));
    }
}
