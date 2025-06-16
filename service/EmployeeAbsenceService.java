package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeAbsenceRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeAbsenceService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeAbsenceRepository absenceRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final PlanningPdfService planningPdfService;
    private final MailService mailService;

    @Transactional
    public EmployeeAbsence addAbsence(EmployeeAbsenceRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employé non trouvé"));

        EmployeeAbsence absence = EmployeeAbsence.builder()
                .employee(employee)
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .approved(false)
                .build();

        EmployeeAbsence saved = absenceRepository.save(absence);

        // ✅ Supprimer toutes les vacations sur cette période
        List<ScheduleAssignment> oldAssignments = assignmentRepository
                .findByEmployeeIdAndDateBetween(employee.getId(), request.getStartDate(), request.getEndDate());

        assignmentRepository.deleteAll(oldAssignments); // ou use setStatus(DECLINED) si tu préfères

        return saved;
    }

    public List<EmployeeAbsence> getEmployeeAbsences(Long employeeId) {
        return absenceRepository.findByEmployeeId(employeeId);
    }
    public boolean isEmployeeAbsentOnDate(Long employeeId, LocalDate date) {
        return absenceRepository
                .existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employeeId, date, date);
    }

    public void handleUnjustifiedAbsence(Long employeeId, LocalDate startTime, LocalDate endTime) {
        boolean alreadyAbsent = absenceRepository.existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId, startTime, endTime);

        if (alreadyAbsent) return;

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employé non trouvé"));

        EmployeeAbsence absence = EmployeeAbsence.builder()
                .employee(employee)
                .startDate(startTime)
                .endDate(endTime)
                .type(AbsenceType.ABSENCE_NON_JUSTIFIEE)
                .approved(false)
                .reason("Absence non justifiée")
                .build();

        absenceRepository.save(absence);

        // Supprimer ses affectations entre start et end
        List<ScheduleAssignment> oldAssignments = assignmentRepository
                .findByEmployeeIdAndDateBetween(employeeId, startTime, endTime);

        oldAssignments.forEach(a -> a.setStatus(ScheduleAssignment.AssignmentStatus.DECLINED));
        assignmentRepository.saveAll(oldAssignments);

        // Rechercher le planning du site pour ce mois
        YearMonth ym = YearMonth.from(startTime); // on suppose que c'est dans le même mois
        Schedule schedule = scheduleRepository
                .findBySiteIdAndMonthAndYear(employee.getSite().getId(), ym.getMonthValue(), ym.getYear())
                .orElse(null);

        if (schedule == null) return;

        // Récupérer les nouvelles affectations de l’employé
        List<ScheduleAssignment> updatedAssignments = assignmentRepository
                .findByScheduleId(schedule.getId())
                .stream()
                .filter(a -> a.getEmployee().getId().equals(employeeId))
                .toList();

        // Générer le PDF mis à jour
        byte[] pdf = planningPdfService.generatePdfForEmployee(schedule, updatedAssignments);

        // Envoyer le mail à l’employé
        String subject = "Mise à jour de votre planning - " + ym.getMonthValue() + "/" + ym.getYear();
        String body = "Bonjour " + employee.getFirstName() + ",\n\n" +
                "Une absence a été enregistrée du " + startTime + " au " + endTime + ".\n" +
                "Voici votre planning mis à jour.\n\nCordialement.";

        mailService.sendSchedulePdfToEmployee(employee.getEmail(), subject, body, pdf);

    }

    public List<EmployeeAbsence> getMonthlyAbsences(Long employeeId, YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return absenceRepository
                .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employeeId, end, start);
    }

    @Transactional
    public EmployeeAbsence updateAbsence(Long id, EmployeeAbsenceRequest request) {
        EmployeeAbsence absence = absenceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Absence non trouvée"));

        absence.setStartDate(request.getStartDate());
        absence.setEndDate(request.getEndDate());
        absence.setType(request.getType());
        absence.setReason(request.getReason());

        return absenceRepository.save(absence);
    }

    @Transactional
    public void deleteAbsence(Long id) {
        if (!absenceRepository.existsById(id)) {
            throw new EntityNotFoundException("Absence non trouvée");
        }
        absenceRepository.deleteById(id);
    }


}
