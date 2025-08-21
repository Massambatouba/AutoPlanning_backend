package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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
    @Query("""
     select s from Schedule s
     where s.company.id = :companyId
       and s.site.id in :siteIds
       and (:month  is null or s.month = :month)
       and (:year   is null or s.year  = :year)
       and (:published is null or s.published = :published)
  """)
    List<Schedule> findByFiltersRestricted(Long companyId, Collection<Long> siteIds,
                                           Integer month, Integer year, Boolean published);

    @Query("""
        select s from Schedule s
         join fetch s.site
         join fetch s.company
        where s.id = :id
     """)
    Optional<Schedule> findByIdWithSiteAndCompany(@Param("id") Long id);


    Optional<Object> findByCompanyIdAndId(Long companyId, Long id);

    List<Schedule> findByCompanyIdAndMonthAndYear(Long companyId, int month, int year);

    long countByCompany_Id(Long companyId);

    List<Schedule> findByCompany_IdOrderByCreatedAtDesc(Long companyId, Pageable page);

    @Query("""
         select coalesce(avg(s.completionRate), 0)
         from   Schedule s
         where  s.company.id = :companyId
         """)
    Double findAverageCompletionRate(@Param("companyId") Long companyId);
}