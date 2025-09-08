// model/Assignment.java
package org.makarimal.projet_gestionautoplanningsecure.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "assignments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(nullable = false)
    private LocalDate date;         // jour concerné

    @Column(nullable = false, length = 5)  // "HH:mm"
    private String startTime;

    @Column(nullable = false, length = 5)  // "HH:mm"
    private String endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private Shift shift;            // optionnel

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentType agentType;    // ton enum existant

    @Column(length = 255)
    private String notes;

    public enum Shift { MATIN, JOUR, SOIR, NUIT }
}
