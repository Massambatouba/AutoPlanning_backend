package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.PlatformSettingsView;
import org.makarimal.projet_gestionautoplanningsecure.dto.UpdatePlatformSettingsRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.PlatformSettings;
import org.makarimal.projet_gestionautoplanningsecure.repository.PlatformSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlatformSettingsService {
    private final PlatformSettingsRepository repo;

    private PlatformSettings getOrCreate() {
        return repo.findById(1L).orElseGet(() -> {
            var s = PlatformSettings.builder()
                    .id(1L)
                    .siteName("Auto Planning")
                    .defaultCurrency("EUR")
                    .vatEnabled(false)
                    .vatRate(BigDecimal.ZERO)
                    .trialDays(14)
                    .paymentProvider("NONE")
                    .passwordMinLength(8)
                    .require2FA(false)
                    //.stripeMode("TEST")
                    .dataRetentionDays(365)
                    .build();
            return repo.save(s);
        });
    }

    public PlatformSettingsView getView() {
        var s = getOrCreate();
        return new PlatformSettingsView(
                s.getSiteName(), s.getLogoUrl(), s.getSupportEmail(),
                s.getDefaultCurrency(), s.getVatRate(), s.isVatEnabled(),
                s.getTrialDays(), s.getDefaultPlanId(),
                s.isRequire2FA(), s.getPasswordMinLength(),
                s.getSmtpHost(), s.getSmtpPort(), s.getSmtpUser(),
                //s.getStripeMode(), s.getStripePublicKey(),
                s.getPaymentProvider(),
                s.getDataRetentionDays()
        );
    }

    @Transactional
    public PlatformSettingsView update(UpdatePlatformSettingsRequest r) {
        var s = getOrCreate();

        if (r.siteName() != null)           s.setSiteName(r.siteName());
        if (r.logoUrl() != null)            s.setLogoUrl(r.logoUrl());
        if (r.supportEmail() != null)       s.setSupportEmail(r.supportEmail());

        if (r.defaultCurrency() != null)    s.setDefaultCurrency(r.defaultCurrency());
        if (r.vatRate() != null)            s.setVatRate(r.vatRate());
        if (r.vatEnabled() != null)         s.setVatEnabled(r.vatEnabled());

        if (r.trialDays() != null)          s.setTrialDays(r.trialDays());
        if (r.defaultPlanId() != null)      s.setDefaultPlanId(r.defaultPlanId());

        if (r.require2FA() != null)         s.setRequire2FA(r.require2FA());
        if (r.passwordMinLength() != null)  s.setPasswordMinLength(r.passwordMinLength());

        if (r.smtpHost() != null)           s.setSmtpHost(r.smtpHost());
        if (r.smtpPort() != null)           s.setSmtpPort(r.smtpPort());
        if (r.smtpUser() != null)           s.setSmtpUser(r.smtpUser());
        if (notBlank(r.smtpPassword()))     s.setSmtpPasswordEnc( encrypt(r.smtpPassword()) );


        if (r.dataRetentionDays() != null)  s.setDataRetentionDays(r.dataRetentionDays());

        repo.save(s);
        return getView();
    }

    private boolean notBlank(String v){ return v != null && !v.isBlank(); }

    // TODO: remplace par un vrai chiffrement (JCE, Jasypt, KMS…)
    private String encrypt(String raw){ return "{enc}" + raw; }
}

