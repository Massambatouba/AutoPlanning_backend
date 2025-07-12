package org.makarimal.projet_gestionautoplanningsecure.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentType agentType;

    private LocalTime startTime;
    private LocalTime endTime;
    private int requiredCount;
    private String notes;

    @ManyToOne
    @JoinColumn(name = "weekly_schedule_rule_id")
    @JsonBackReference
    private WeeklyScheduleRule weeklyScheduleRule;
}

