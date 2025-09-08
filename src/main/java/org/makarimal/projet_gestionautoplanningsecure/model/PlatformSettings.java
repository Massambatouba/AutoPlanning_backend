package org.makarimal.projet_gestionautoplanningsecure.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "platform_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlatformSettings {
    @Id
    private Long id;

    // Branding
    private String siteName;
    private String logoUrl;
    private String supportEmail;

    // Billing
    private String paymentProvider;
    private String defaultCurrency;
    @Column(precision = 10, scale = 4)
    private BigDecimal vatRate;
    private boolean vatEnabled;

    // Subscriptions
    private Integer trialDays;
    private Long defaultPlanId;

    // Security
    private boolean require2FA;
    private Integer passwordMinLength;

    // SMTP (secrets stockés chiffrés si possible)
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUser;
    private String smtpPasswordEnc;

    // Stripe (idem)
    private String stripeMode;
    private String stripePublicKey;
    private String stripeSecretKeyEnc;

    private Integer dataRetentionDays;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate(){ updatedAt = LocalDateTime.now(); }
    @PreUpdate  void onUpdate(){ updatedAt = LocalDateTime.now(); }
}
