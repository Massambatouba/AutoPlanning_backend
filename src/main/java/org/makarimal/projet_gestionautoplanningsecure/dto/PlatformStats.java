package org.makarimal.projet_gestionautoplanningsecure.dto;

public record PlatformStats(
        long totalCompanies,
        long activeCompanies,
        long totalUsers,
        double totalRevenue,
        double monthlyGrowth
) {}
