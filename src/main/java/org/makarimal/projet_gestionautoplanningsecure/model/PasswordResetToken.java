package org.makarimal.projet_gestionautoplanningsecure.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    private User user;
    @Column(unique = true, nullable=false)
    private String token;
    @Column(nullable=false)
    private Instant expiresAt;
    private Instant usedAt;
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    public boolean isUsed() {
        return usedAt != null;
    }
}
