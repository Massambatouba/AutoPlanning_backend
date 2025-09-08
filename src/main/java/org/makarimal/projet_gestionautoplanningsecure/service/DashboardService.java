package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.PlatformStats;
import org.makarimal.projet_gestionautoplanningsecure.dto.RevenueData;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.PaymentRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository companyRepo;
    private final UserRepository userRepo;
    private final PaymentRepository paymentRepo;

    public PlatformStats platformStats() {
        long totalCompanies = companyRepo.count();
        long activeCompanies = companyRepo.countByActiveTrue();
        long totalUsers = userRepo.count();
        double totalRevenue = Optional.ofNullable(
                companyRepo.sumMonthlyRevenueByStatus(Company.SubscriptionStatus.ACTIVE)
        ).orElse(0.0);
        double monthlyGrowth = monthlyGrowthPercent(); // ← on passe par Java
        return new PlatformStats(totalCompanies, activeCompanies, totalUsers, totalRevenue, monthlyGrowth);
    }

    // Service
    public List<RevenueData> revenueLastMonths(int months) {

        LocalDateTime untilExclusive = LocalDate.now().withDayOfMonth(1).atStartOfDay();


        // LocalDateTime untilExclusive = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();

        LocalDateTime fromInclusive = untilExclusive.minusMonths(months);

        // Ta requête agrégée par mois (ne renvoie que les mois avec paiements)
        var rows = paymentRepo.monthlyRevenueBetween(fromInclusive, untilExclusive);
        // rows doit exposer getMonth() => "YYYY-MM" (ou YearMonth) et getRevenue() => BigDecimal

        // Map pour accès rapide
        Map<String, BigDecimal> byMonth = rows.stream()
                .collect(Collectors.toMap(
                        r -> r.getMonth().toString(),            // si getMonth() renvoie YearMonth
                        // r -> r.getMonth(),                    // si c'est déjà "YYYY-MM"
                        r -> r.getRevenue()
                ));

        // Génère exactement N mois continus avec 0 si absent
        List<RevenueData> out = new ArrayList<>();
        var startYm = java.time.YearMonth.from(fromInclusive); // premier mois inclus
        for (int i = 0; i < months; i++) {
            var ym = startYm.plusMonths(i);
            var key = ym.toString(); // "2025-08"
            var amount = byMonth.getOrDefault(key, java.math.BigDecimal.ZERO);
            out.add(new RevenueData(key, amount));
        }
        return out;
    }


    // Croissance (M vs M-1) calculée en Java (simple, sûr)
    public double monthlyGrowthPercent() {
        LocalDateTime currentStart  = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousStart = currentStart.minusMonths(1);
        LocalDateTime nextStart     = currentStart.plusMonths(1);

        BigDecimal prev = paymentRepo.sumBetween(previousStart, currentStart);
        BigDecimal curr = paymentRepo.sumBetween(currentStart, nextStart);

        if (prev == null || prev.signum() == 0) return 0d;

        BigDecimal ratio = curr.subtract(prev)
                .divide(prev, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return ratio.doubleValue();
    }


}
