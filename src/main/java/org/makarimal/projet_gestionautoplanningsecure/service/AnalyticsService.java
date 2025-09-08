package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.AnalyticsSummary;
import org.makarimal.projet_gestionautoplanningsecure.dto.RevenueData;
import org.makarimal.projet_gestionautoplanningsecure.dto.RevenuePoint;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final CompanyRepository companyRepo;
    private final PaymentRepository paymentRepo; // ou ce que tu utilises déjà

    public List<RevenuePoint> revenueSeries(int months) {
        LocalDateTime until = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime from  = until.minusMonths(months - 1);

        var revenues = paymentRepo.monthlyRevenueBetween(from, until); // List<MonthRevenueDto> {month: '2025-04', revenue: BigDecimal}
        var newCos   = companyRepo.newCompaniesMonthly(from, until);   // List<Object[]> {ym, count}

        Map<String, Long> byMonthNew = newCos.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).longValue()));

        return revenues.stream()
                .map(r -> new RevenuePoint(r.getMonth(), r.getRevenue(), byMonthNew.getOrDefault(r.getMonth(), 0L)))
                .toList();
    }

    public AnalyticsSummary summary(int months) {
        var mrr = companyRepo.sumActiveMRR();

        // Growth vs période précédente
        var currentSeries = revenueSeries(months);
        var previousSeries = revenueSeries(months); // <= à adapter si tu veux T-1 période (from-previous, until-previous)
        BigDecimal mrrPrev = previousSeries.stream()
                .map(RevenuePoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mrrNow = currentSeries.stream()
                .map(RevenuePoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal mrrGrowthPct =
                (mrrPrev.compareTo(BigDecimal.ZERO) == 0)
                        ? BigDecimal.ZERO
                        : mrrNow.subtract(mrrPrev).multiply(BigDecimal.valueOf(100)).divide(mrrPrev, 1, RoundingMode.HALF_UP);

        // Nouvelles entreprises sur la période
        LocalDateTime until = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime from  = until.minusMonths(months - 1);
        long newCompanies = companyRepo.countByCreatedAtBetween(from, until);

        // Churn approx
        long expired = companyRepo.countBySubscriptionStatusAndUpdatedAtBetween(Company.SubscriptionStatus.EXPIRED, from, until);
        // actifs actuels pour normaliser
        long actifsActuels = 0 /* ...compte c où status=ACTIVE ... */;
        BigDecimal churnPct = actifsActuels == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(expired * 100.0 / actifsActuels).setScale(1, RoundingMode.HALF_UP);

        // ARPU: MRR / nb utilisateurs (si dispo) – sinon ARPC: MRR / nb entreprises actives
        long usersActifs = 0 /* récupère nb utilisateurs actifs (ou totalUsers) */;
        BigDecimal arpu = (usersActifs == 0)
                ? BigDecimal.ZERO
                : mrr.divide(BigDecimal.valueOf(usersActifs), 2, RoundingMode.HALF_UP);

        // distribution des plans
        Map<String, Long> planDist = companyRepo.countByPlan().stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).longValue()));

        return new AnalyticsSummary(mrr, mrrGrowthPct, newCompanies, churnPct, arpu, planDist);
    }
}
