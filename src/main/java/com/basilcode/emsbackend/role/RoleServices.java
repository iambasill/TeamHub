package com.basilcode.emsbackend.role;

import com.basilcode.emsbackend.common.exception.AlreadyExistsException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.role.dto.RoleRequest;
import com.basilcode.emsbackend.role.dto.RoleResponse;
import com.basilcode.emsbackend.role.entity.Role;
import com.basilcode.emsbackend.role.mapper.RoleMapper;
import com.basilcode.emsbackend.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServices {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        roleRepository.findByName(request.getName()).ifPresent(r -> {
            throw new AlreadyExistsException("Role already exists with name: " + request.getName());
        });

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        log.info("Role created with id: {}", saved.getId());
        return roleMapper.toResponse(saved);
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    public RoleResponse getRole(UUID id) {
        return roleMapper.toResponse(getRoleEntity(id));
    }

    @Transactional
    public RoleResponse updateRole(UUID id, RoleRequest request) {
        Role role = getRoleEntity(id);

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        Role updated = roleRepository.save(role);
        log.info("Role updated with id: {}", updated.getId());
        return roleMapper.toResponse(updated);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = getRoleEntity(id);
        roleRepository.delete(role);
        log.info("Role deleted with id: {}", id);
    }

    /**
     * Returns the managed entity for use by other services (e.g. Employee) that need
     * to attach a Role without reaching into RoleRepository directly.
     */
    public Role getRoleEntity(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
    }

    public Set<Role> getRoleEntities(Set<UUID> ids) {
        return new HashSet<>(roleRepository.findAllById(ids));
    }
}
