package org.makarimal.projet_gestionautoplanningsecure.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "site_schedule_rules")
public class SiteScheduleRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ElementCollection
    @CollectionTable(name = "site_working_days", joinColumns = @JoinColumn(name = "rule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private List<DayOfWeek> workingDays;

    @Column(nullable = false)
    private Integer minEmployeesPerDay;

    @Column(nullable = false)
    private Integer maxEmployeesPerDay;

    private boolean requiresNightShift;
    private boolean requiresWeekendCoverage;

    @Column(nullable = false)
    private Integer minExperienceLevel;

    @ElementCollection
    @CollectionTable(name = "site_required_skills", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "skill")
    private List<String> requiredSkills;
}