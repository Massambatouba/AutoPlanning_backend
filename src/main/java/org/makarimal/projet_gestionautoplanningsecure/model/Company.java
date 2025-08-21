package org.makarimal.projet_gestionautoplanningsecure.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "companies")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120, nullable = false)
    private String name;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnoreProperties("company")
    private List<Site> sites;

    private String address;
    private String phone;
    @Email(message = "Invalid email format")
    private String email;
    private String website;
    private String logoUrl;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;

    private LocalDateTime subscriptionExpiresAt;
    private Integer maxEmployees;
    private Integer maxSites;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "company")
    @JsonIgnoreProperties("company")
    private List<User> employees;   // pour employeesCount

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "monthly_revenue", nullable = false,
            columnDefinition = "double precision default 0")
    @Builder.Default
    private double monthlyRevenue = 0d;
    private Instant lastLoginAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }



    public enum SubscriptionStatus {
        ACTIVE, INACTIVE, TRIAL
    }
}

