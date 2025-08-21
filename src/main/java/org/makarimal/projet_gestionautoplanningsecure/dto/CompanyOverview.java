package org.makarimal.projet_gestionautoplanningsecure.dto;

import org.makarimal.projet_gestionautoplanningsecure.model.Company;

import java.time.Instant;
import java.time.LocalDateTime;

public record CompanyOverview(
        Long id,
        String name,
        String email,
        Long subscriptionPlanId,   // <-- l’ID du plan
        String subscriptionStatus, // ACTIVE | TRIAL | EXPIRED | INACTIVE
        String subscriptionPlan,   // libellé du plan (ex. STARTER/PRO/ENTERPRISE)
        double monthlyRevenue,
        long employeesCount,
        long sitesCount,
        LocalDateTime createdAt,
        Instant lastLoginAt,
        boolean isActive
) {
    /** Fabrique actuelle (utilise les collections si déjà chargées) */
    public static CompanyOverview of(Company c) {
        long emp = (c.getEmployees() != null) ? c.getEmployees().size() : 0L;
        long sites = (c.getSites() != null) ? c.getSites().size() : 0L;
        return of(c, emp, sites);
    }

    /** ✅ Nouvelle fabrique : utilise les compteurs remontés par les repositories */
    public static CompanyOverview of(Company c, long employeesCount, long sitesCount) {
        return new CompanyOverview(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getSubscriptionPlan() != null ? c.getSubscriptionPlan().getId() : null,        // Long OK
                c.getSubscriptionStatus() != null ? c.getSubscriptionStatus().name() : null,      // String OK
                c.getSubscriptionPlan() != null ? c.getSubscriptionPlan().getName() : null,       // String OK
                c.getMonthlyRevenue(),                                                             // double OK
                employeesCount,
                sitesCount,
                c.getCreatedAt(),
                c.getLastLoginAt(),
                c.isActive()
        );
    }
}
