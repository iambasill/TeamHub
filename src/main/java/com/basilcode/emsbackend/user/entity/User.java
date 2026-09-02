package com.basilcode.emsbackend.user.entity;

import com.basilcode.emsbackend.employee.entity.Employee;
import com.basilcode.emsbackend.role.entity.Role;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name" , nullable = false)
    private String lastName;

    @Column(name = "email_id", nullable = false, unique = true)
    private String emailId;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Builder.Default
    @Column(name = "is_account_non_locked")
    private Boolean isAccountNonLocked =  Boolean.TRUE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "userType")
    private UserTypeEnum userType = UserTypeEnum.EMPLOYEE;

    @Column(name = "password" , nullable = false)
    private String password;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = Boolean.TRUE;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Employee employee;

    public Set<Role> getRoles() {
        return employee != null ? employee.getRoles() : Set.of();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return isAccountNonLocked; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    /**
     * Every account carries the baseline ROLE_EMPLOYEE authority regardless of userType,
     * since HR/MANAGER/ADMIN staff are still employees who submit their own leave,
     * clock their own attendance, and view their own payslips. SUPER_ADMIN additionally
     * carries ROLE_ADMIN so admin-only endpoints don't need special-casing it.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserTypeEnum type = userType != null ? userType : UserTypeEnum.EMPLOYEE;
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + type.name()));
        if (type == UserTypeEnum.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return emailId;
    }

}



