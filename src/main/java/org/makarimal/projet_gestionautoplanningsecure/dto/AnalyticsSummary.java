package org.makarimal.projet_gestionautoplanningsecure.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsSummary(
        BigDecimal mrr,           // MRR actuel (€/mois)
        BigDecimal mrrGrowthPct,  // croissance % vs période précédente
        long newCompanies,        // nouvelles entreprises sur la période
        BigDecimal churnPct,      // churn % (approx si pas d’historique)
        BigDecimal arpu,          // ARPU = MRR / nb utilisateurs actifs (ou ARPC si tu préfères)
        Map<String, Long> planDistribution // { "STARTER": 12, "PRO": 8, ... }
) {}