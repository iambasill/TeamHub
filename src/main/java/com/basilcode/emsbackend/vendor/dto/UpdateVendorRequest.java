package com.basilcode.emsbackend.vendor.dto;

/** Every field optional — only non-null ones are applied, same "partial update" convention as
 * {@code EmployeeServices#updateEmployee}. */
public record UpdateVendorRequest(
        String name,
        String category,
        String contactName,
        String contactEmail,
        String contactPhone,
        String notes
) {
}
