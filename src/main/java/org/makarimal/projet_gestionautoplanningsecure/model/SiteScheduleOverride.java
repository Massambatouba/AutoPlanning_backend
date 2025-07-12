package org.makarimal.projet_gestionautoplanningsecure.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "site_schedule_overrides")
public class SiteScheduleOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    private LocalDate overrideDate;

    private Integer minEmployees;
    private Integer maxEmployees;
    private Integer minExperienceLevel;

    private boolean requiresNightShift;
    private boolean requiresWeekendCoverage;

    @ElementCollection
    @CollectionTable(name = "override_required_skills", joinColumns = @JoinColumn(name = "override_id"))
    @Column(name = "skill")
    private List<String> requiredSkills;
}

