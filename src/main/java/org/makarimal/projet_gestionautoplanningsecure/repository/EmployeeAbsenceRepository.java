package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.Absence;
import org.makarimal.projet_gestionautoplanningsecure.model.AbsenceType;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface EmployeeAbsenceRepository extends JpaRepository<EmployeeAbsence, Long> {

    // Vérifie si un employé est absent un jour précis
    /*
    boolean existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId, LocalDate date1, LocalDate date2
    );

     */


    List<EmployeeAbsence> findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long employeeId, LocalDate startTime, LocalDate endTime);

    // Récupère toutes les absences d'un employé sur une période donnée

    /* ─── nouvelle signature « IN » ─── */
    List<EmployeeAbsence>
    findByEmployeeIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Collection<Long> employeeIds,   // ← Collection ou List
            LocalDate endDate,
            LocalDate startDate);

    boolean existsByEmployeeIdAndDate(Long employeeId, LocalDate date);

    boolean existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            LocalDate endDate,   // ← on passera "date" deux fois
            LocalDate startDate);


    // Récupérer toutes les absences d’un employé
    List<EmployeeAbsence> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndTypeInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            List<AbsenceType> types,
            LocalDate startDate,
            LocalDate endDate
    );


}
