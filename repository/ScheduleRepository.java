package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByCompanyId(Long companyId);

    List<Schedule> findBySiteId(Long siteId);

    Optional<Schedule> findBySiteIdAndMonthAndYear(Long siteId, int month, int year);

    @Query("""
        SELECT s
        FROM   Schedule s
        WHERE  s.company.id = :companyId
          AND (:siteId      IS NULL OR s.site.id      = :siteId)
          AND (:month       IS NULL OR s.month        = :month)
          AND (:year        IS NULL OR s.year         = :year)
          AND (:published   IS NULL OR s.published    = :published)
    """)
    List<Schedule> findByFilters(@Param("companyId") Long companyId,
                                 @Param("siteId")    Long siteId,
                                 @Param("month")     Integer month,
                                 @Param("year")      Integer year,
                                 @Param("published") Boolean published);

    Optional<Object> findByCompanyIdAndId(Long companyId, Long id);
}