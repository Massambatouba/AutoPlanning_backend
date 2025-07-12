package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceService {

    private final EmployeeAbsenceRepository absenceRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final ScheduleRepository scheduleRepository;


    /**
     * Gère une absence non justifiée :
     * 1. enregistre l’absence,
     * 2. annule les affectations du jour,
     * 3. recalcule le taux de complétion des plannings impactés.
     */
    @Transactional
    public void handleUnjustifiedAbsence(Long employeeId, LocalDate startTime, LocalDate endTime) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee " + employeeId + " not found"));

        for (LocalDate date = startTime; !date.isAfter(endTime); date = date.plusDays(1)) {
            boolean alreadyExists = absenceRepository.existsByEmployeeIdAndDate(employeeId, date);
            if (alreadyExists) continue;

            EmployeeAbsence absence = EmployeeAbsence.builder()
                    .employee(employee)
                    .date(date)
                    .type(AbsenceType.ABSENCE_NON_JUSTIFIEE)
                    .build();
            absenceRepository.save(absence);

            List<ScheduleAssignment> assignments = assignmentRepository.findByEmployeeIdAndDate(employeeId, date);
            assignments.forEach(a -> a.setStatus(ScheduleAssignment.AssignmentStatus.DECLINED));
            assignmentRepository.saveAll(assignments);

            assignments.stream()
                    .map(ScheduleAssignment::getSchedule)
                    .distinct()
                    .forEach(this::recomputeCompletionRate);
        }
    }


    /* ----- utilitaire interne ----- */
    private void recomputeCompletionRate(Schedule schedule) {
        List<ScheduleAssignment> all = assignmentRepository.findByScheduleId(schedule.getId());

        long confirmed = all.stream()
                .filter(a -> a.getStatus() == ScheduleAssignment.AssignmentStatus.CONFIRMED)
                .count();

        int rate = all.isEmpty() ? 0 : (int)(100.0 * confirmed / all.size());
        schedule.setCompletionRate(rate);

        scheduleRepository.save(schedule);
    }

}
