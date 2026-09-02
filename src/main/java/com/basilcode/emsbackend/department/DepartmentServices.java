package com.basilcode.emsbackend.department;

import com.basilcode.emsbackend.common.exception.AlreadyExistsException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.department.dto.DepartmentRequest;
import com.basilcode.emsbackend.department.dto.DepartmentResponse;
import com.basilcode.emsbackend.department.entity.Department;
import com.basilcode.emsbackend.department.mapper.DepartmentMapper;
import com.basilcode.emsbackend.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServices {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        departmentRepository.findByName(request.getName()).ifPresent(d -> {
            throw new AlreadyExistsException("Department already exists with name: " + request.getName());
        });

        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());

        Department saved = departmentRepository.save(department);
        log.info("Department created with id: {}", saved.getId());
        return departmentMapper.toResponse(saved);
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }

    public DepartmentResponse getDepartment(UUID id) {
        return departmentMapper.toResponse(getDepartmentEntity(id));
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request) {
        Department department = getDepartmentEntity(id);

        if (request.getName() != null) {
            department.setName(request.getName());
        }
        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }

        Department updated = departmentRepository.save(department);
        log.info("Department updated with id: {}", updated.getId());
        return departmentMapper.toResponse(updated);
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        Department department = getDepartmentEntity(id);
        departmentRepository.delete(department);
        log.info("Department deleted with id: {}", id);
    }

    /**
     * Returns the managed entity for use by other services (e.g. Employee) that need
     * to attach a Department without reaching into DepartmentRepository directly.
     */
    public Department getDepartmentEntity(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found with id: " + id));
    }
}
