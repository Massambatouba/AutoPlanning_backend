package org.makarimal.projet_gestionautoplanningsecure.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employees")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    private String city;
    private String zipCode;
    private String adress;
    private String country;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String firstName;


    @OneToOne(mappedBy = "employee",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @JsonManagedReference
    private EmployeePreference preference;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String position;

    private String department;

    @Column(nullable = false, unique = true)
    private String employeeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType contractType;

    @Column(nullable = false)
    private Integer maxHoursPerWeek;

    @ManyToOne
    @JoinColumn(name = "site_id")
    @JsonIgnore
    private Site site;

    @ElementCollection
    @CollectionTable(name = "employee_preferred_sites", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "site_id")
    private List<Long> preferredSites;

    @ElementCollection
    @CollectionTable(name = "employee_skill_sets", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "skill")
    private List<String> skillSets;

    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_agent_types",
            joinColumns = @JoinColumn(name = "employee_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type")
    private Set<AgentType> agentTypes = new HashSet<>();


    public enum ContractType {
        FULL_TIME, PART_TIME, TEMPORARY, CONTRACT
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

