package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.Assignment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("""
    select a
    from Assignment a
      join a.schedule s
      join s.site si
    where a.employee.id = :employeeId
      and a.date between :start and :end
      and (
        si.company.id = :companyId
        or (si.company is null and a.employee.company.id = :companyId)
      )
  """)
    List<Assignment> findEmployeeAssignmentsInPeriodForTenant(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("companyId") Long companyId
    );

}
