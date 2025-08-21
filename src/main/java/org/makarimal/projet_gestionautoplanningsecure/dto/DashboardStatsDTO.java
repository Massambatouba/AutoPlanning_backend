package org.makarimal.projet_gestionautoplanningsecure.dto;

// DTO compact pour le tableau de bord
public record DashboardStatsDTO(
        long schedulesCount,
        long employeesCount,
        long sitesCount,
        double  completionRate      // 0‑100 %
) {}

