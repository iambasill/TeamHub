package com.basilcode.emsbackend.attendance.entity;

import com.basilcode.emsbackend.attendance.enums.AttendanceStatus;
import com.basilcode.emsbackend.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "check_in", nullable = false)
    private OffsetDateTime checkIn;

    @Column(name = "check_out")
    private OffsetDateTime checkOut;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = AttendanceStatus.PRESENT;
        }
    }
}