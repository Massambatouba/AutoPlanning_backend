package org.makarimal.projet_gestionautoplanningsecure.dto;

import java.math.BigDecimal;

public record PlatformSettingsView(
        String siteName,
        String logoUrl,
        String supportEmail,
        String defaultCurrency,
        BigDecimal vatRate,
        boolean vatEnabled,
        Integer trialDays,
        Long defaultPlanId,
        boolean require2FA,
        Integer passwordMinLength,
        String smtpHost,
        Integer smtpPort,
        String smtpUser,
        //String stripeMode,
        String paymentProvider,
        //String stripePublicKey,
        Integer dataRetentionDays
) {
}
