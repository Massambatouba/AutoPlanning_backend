package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.PlatformStats;
import org.makarimal.projet_gestionautoplanningsecure.dto.RevenueData;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.PaymentRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        double totalRevenue = paymentRepo.sumRevenueLastMonth();
        double monthlyGrowth = monthlyGrowthPercent(); // ← on passe par Java
        return new PlatformStats(totalCompanies, activeCompanies, totalUsers, totalRevenue, monthlyGrowth);
    }

    // Revenus des n derniers mois (inclus => jusqu’au début du mois courant)
    public List<RevenueData> revenueLastMonths(int months) {
        LocalDateTime until = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime from  = until.minusMonths(months - 1);
        return paymentRepo.monthlyRevenueBetween(from, until)
                .stream()
                .map(v -> new RevenueData(v.getMonth(), v.getRevenue()))
                .toList();
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
