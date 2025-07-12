package org.makarimal.projet_gestionautoplanningsecure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(
        name = "schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_site_month_year",
                columnNames = {"site_id", "month", "year"}      // 1 seul planning par mois & site
        )
)
public class Schedule {

    
    /* ----------  Identité  ---------- */

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ----------  Relations  ---------- */

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @OneToMany(mappedBy = "schedule",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default                       // évite le NPE quand on utilise le builder
    private List<ScheduleAssignment> assignments = new ArrayList<>();

    /* ----------  Données métier  ---------- */

    @Column(nullable = false)
    private String name;                   // ex. « Arpajon – 05/2025 »

    @Column(nullable = false)             // 1‑12
    private Integer month;

    @Column(nullable = false)             // ex. 2025
    private Integer year;

    @Builder.Default
    private boolean published = false;
    @Column(nullable = false)
    private boolean validated = false;

    @Builder.Default
    private boolean sent = false;

    private LocalDateTime sentAt;

    @Builder.Default                      // % d’assignations confirmées
    private Integer completionRate = 0;

    /* ----------  Audit  ---------- */

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
