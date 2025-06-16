package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.Absence;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAbsence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    boolean existsByEmployeeIdAndDate(Long employeeId, LocalDate date);


    List<Absence> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate first, LocalDate last);

   // List<EmployeeAbsence> findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long employeeId, LocalDate last, LocalDate first);
}
