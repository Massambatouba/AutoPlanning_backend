package org.makarimal.projet_gestionautoplanningsecure.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_preferences")
public class EmployeePreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonBackReference
    private Employee employee;

    private boolean canWorkWeekends;
    private boolean canWorkWeeks;
    private boolean canWorkNights;
    private boolean prefersDay;
    private boolean prefersNight;
    private boolean noPreference;

    @Column(nullable = false)
    private Integer minHoursPerDay;

    @Column(nullable = false)
    private Integer maxHoursPerDay;

    @Column(nullable = false)
    private Integer minHoursPerWeek;

    @Column(nullable = false)
    private Integer maxHoursPerWeek;

    private Integer preferredConsecutiveDays;
    private Integer minConsecutiveDaysOff;


}