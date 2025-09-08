package org.makarimal.projet_gestionautoplanningsecure.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "employee_documents")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EmployeeDocumentCategory category;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EmployeeDocumentType type;

    private String number;
    private LocalDate issuedAt;
    private LocalDate expiryDate;

    /** URL ou path du fichier stocké (à adapter à ton FileStorageService) */
    private String fileUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void pre() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate  void up()  { updatedAt = LocalDateTime.now(); }
}


