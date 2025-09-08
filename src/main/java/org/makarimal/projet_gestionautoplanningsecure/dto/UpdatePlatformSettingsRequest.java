package org.makarimal.projet_gestionautoplanningsecure.dto;

import java.math.BigDecimal;

public record UpdatePlatformSettingsRequest(
        String siteName,
        String logoUrl,
        String supportEmail,
        String defaultCurrency,
        BigDecimal vatRate,
        Boolean vatEnabled,
        Integer trialDays,
        Long defaultPlanId,
        Boolean require2FA,
        String paymentProvider,
        Integer passwordMinLength,
        String smtpHost,
        Integer smtpPort,
        String smtpUser,
        String smtpPassword,
        //String stripeMode,
        //String stripePublicKey,
        //String stripeSecretKey,
        Integer dataRetentionDays
) {}
