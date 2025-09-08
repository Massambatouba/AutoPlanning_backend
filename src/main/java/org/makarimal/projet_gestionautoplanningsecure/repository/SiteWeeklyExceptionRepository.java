package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.SiteWeeklyException;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SiteWeeklyExceptionRepository extends JpaRepository<SiteWeeklyException, Long> {

    // 1) Toutes les exceptions d’un site (tri réverse par date de début)
    List<SiteWeeklyException> findBySiteIdOrderByStartDateDesc(Long siteId);

    // 2) Exceptions qui chevauchent l’intervalle [from; to]
    @Query("""
           select e
           from SiteWeeklyException e
           where e.site.id = :siteId
             and e.startDate <= :to
             and e.endDate   >= :from
           order by e.startDate desc
           """)
    List<SiteWeeklyException> findOverlapping(
            @Param("siteId") Long siteId,
            @Param("from")    LocalDate from,
            @Param("to")      LocalDate to
    );

    // (facultatif) garde l’ancien finder si tu l’utilises ailleurs
    List<SiteWeeklyException> findBySiteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long siteId, LocalDate date1, LocalDate date2);
}
