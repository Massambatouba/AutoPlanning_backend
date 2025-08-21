package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.dto.MonthlyRevenueView;
import org.makarimal.projet_gestionautoplanningsecure.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
           select coalesce(sum(p.amount), 0)
           from Payment p
           where p.date >= :start
           """)
    double sumRevenueSince(@Param("start") LocalDateTime start);

    default double sumRevenueLastMonth() {
        return sumRevenueSince(LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay());
    }

    // Somme sur un intervalle [from, until[
    @Query("""
           select coalesce(sum(p.amount), 0)
           from Payment p
           where p.date >= :from and p.date < :until
           """)
    BigDecimal sumBetween(@Param("from") LocalDateTime from,
                          @Param("until") LocalDateTime until);

    // Histogramme mensuel via projection interface (plus de "new …")
    @Query("""
        select function('to_char', p.date, 'YYYY-MM') as month,
               sum(p.amount)                           as revenue
        from Payment p
        where p.date >= :from and p.date < :until
        group by function('to_char', p.date, 'YYYY-MM')
        order by 1
        """)
    List<MonthlyRevenueView> monthlyRevenueBetween(@Param("from")  LocalDateTime from,
                                                   @Param("until") LocalDateTime until);
}
