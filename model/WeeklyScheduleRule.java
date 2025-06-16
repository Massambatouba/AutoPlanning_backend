package org.makarimal.projet_gestionautoplanningsecure.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_schedule_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyScheduleRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    private int minEmployees;
    private int maxEmployees;
    private int minExperienceLevel;
    private boolean requiresNightShift;
    private boolean requiresWeekendCoverage;


    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> requiredSkills;

    @OneToMany(mappedBy = "weeklyScheduleRule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<AgentSchedule> agents;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "weekly_rule_required_agent_types",
            joinColumns = @JoinColumn(name = "rule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type")
    private List<AgentType> requiredAgentTypes = new ArrayList<>();



}
