package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.ScheduleAssignment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleAssignmentRepository
        extends JpaRepository<ScheduleAssignment, Long> {

    /* ---------- accès “bruts” ---------- */
    List<ScheduleAssignment> findByScheduleId(Long scheduleId);
    List<ScheduleAssignment> findByEmployeeId(Long employeeId);

    /* ---------- filtrage chronologique ---------- */
    List<ScheduleAssignment> findByEmployeeIdAndDateBetween(
            Long employeeId, LocalDate start, LocalDate end);

    List<ScheduleAssignment> findBySchedule_Site_IdAndDateBetween(
            Long siteId, LocalDate start, LocalDate end);

    boolean existsByEmployeeIdAndDate(Long employeeId, LocalDate date);

    /* ---------- requêtes JPQL utilitaires ---------- */
    @Query("""
           select sa
           from ScheduleAssignment sa
           where sa.schedule.company.id = :companyId
           """)
    List<ScheduleAssignment> findAllForCompany(@Param("companyId") Long companyId);

    @Query("""
           select sa
           from ScheduleAssignment sa
           where sa.schedule.site.id = :siteId
             and sa.schedule.month = :month
             and sa.schedule.year  = :year
           """)
    List<ScheduleAssignment> findBySiteMonthYear(@Param("siteId") Long siteId,
                                                 @Param("month")   Integer month,
                                                 @Param("year")    Integer year);

    /* ---------- nettoyage ---------- */
    void deleteByScheduleId(Long scheduleId);
    void deleteByEmployeeIdAndDate(Long employeeId, LocalDate date);


    List<ScheduleAssignment> findBySchedule_Company_Id(Long id);

    // ScheduleAssignmentRepository.java

    @Query("""
    SELECT sa FROM ScheduleAssignment sa
    WHERE sa.employee.id = :employeeId
      AND sa.schedule.month = :month
      AND sa.schedule.year = :year
""")
    List<ScheduleAssignment> findByEmployeeAndScheduleMonthAndYear(
            @Param("employeeId") Long employeeId,
            @Param("month") int month,
            @Param("year") int year
    );


    List<ScheduleAssignment> findByEmployeeIdAndDate(Long employeeId, LocalDate date);


    List<ScheduleAssignment> findByScheduleIdAndEmployeeId(Long scheduleId, Long employeeId);
}
