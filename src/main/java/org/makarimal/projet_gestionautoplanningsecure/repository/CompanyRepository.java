package org.makarimal.projet_gestionautoplanningsecure.repository;


import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    long countByActiveTrue();

    @Query(value = """
        select c.*
        from companies c
        where (:q is null or c.name ILIKE ('%' || :q || '%'))
          and (:status is null or c.subscription_status = :status)
        order by c.created_at desc
        """,
            countQuery = """
        select count(*)
        from companies c
        where (:q is null or c.name ILIKE ('%' || :q || '%'))
          and (:status is null or c.subscription_status = :status)
        """,
            nativeQuery = true)
    Page<Company> searchNative(@Param("q") String q,
                               @Param("status") String status,  // <- chaîne ENUM en .name()
                               Pageable page);

    @Query("""
     select coalesce(sum(sp.price), 0)
     from Company c
     join c.subscriptionPlan sp
     where c.subscriptionStatus = :status
  """)
    Double sumMonthlyRevenueByStatus(@Param("status") Company.SubscriptionStatus status);

    long countBySubscriptionStatus(Company.SubscriptionStatus status);

    @Query("""
    select coalesce(sum(sp.price), 0)
    from Company c
    join c.subscriptionPlan sp
    where c.subscriptionStatus = org.makarimal.projet_gestionautoplanningsecure.model.Company$SubscriptionStatus.ACTIVE
  """)
    java.math.BigDecimal sumActiveMonthlyRevenue();

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("select c.subscriptionPlan.name, count(c) from Company c group by c.subscriptionPlan.name")
    List<Object[]> countByPlan();

    // MRR = somme des prix des plans des entreprises ACTIVES
    @Query("select coalesce(sum(p.price), 0) from Company c join c.subscriptionPlan p where c.subscriptionStatus = 'ACTIVE'")
    BigDecimal sumActiveMRR();

    // Si tu as un champ updatedAt et tu considères le churn = passées à EXPIRED sur la période :
    long countBySubscriptionStatusAndUpdatedAtBetween(
            Company.SubscriptionStatus status,
            LocalDateTime from,
            LocalDateTime until
    );

    @Query("""
    select to_char(c.createdAt, 'YYYY-MM') as ym, count(c)
    from Company c
    where c.createdAt >= :from and c.createdAt < :until
    group by ym
    order by ym
  """)
    List<Object[]> newCompaniesMonthly(LocalDateTime from, LocalDateTime until);

    @Query(value = """
        SELECT to_char(c.created_at, 'YYYY-MM') AS ym,
               COUNT(*)                         AS cnt
        FROM companies c
        WHERE c.created_at >= :from AND c.created_at < :until
        GROUP BY ym
        ORDER BY ym
    """, nativeQuery = true)
    List<Object[]> newCompaniesBetween(@Param("from") LocalDateTime from,
                                       @Param("until") LocalDateTime until);

}