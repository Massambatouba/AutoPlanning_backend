package org.makarimal.projet_gestionautoplanningsecure.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

// SiteWeeklyException.java
@Entity
@Table(name = "site_weekly_exceptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteWeeklyException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "site_id")
    private Site site;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private WeeklyExceptionType type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate; // inclusif

    /** Optionnel : restreindre aux jours de la semaine (sinon tous) */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "weekly_exc_days", joinColumns = @JoinColumn(name = "exc_id"))
    @Column(name = "dow")
    private Set<DayOfWeek> daysOfWeek;

    /** Pour ADD/REPLACE/MASK : critères/valeurs */
    @Enumerated(EnumType.STRING)
    private AgentType agentType;      // optionnel pour MASK (si null => tous)

    private LocalTime startTime;      // pour ADD/REPLACE (valeur), pour MASK (critère)
    private LocalTime endTime;

    private Integer requiredCount;    // pour ADD/REPLACE
    private Integer minExperience;    // optionnel (fit)

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "weekly_exc_skills", joinColumns = @JoinColumn(name = "exc_id"))
    private List<String> requiredSkills;

    private String notes;
}

