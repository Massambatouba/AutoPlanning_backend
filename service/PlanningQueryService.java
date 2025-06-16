package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.Absence;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAbsence;
import org.makarimal.projet_gestionautoplanningsecure.model.ScheduleAssignment;
import org.makarimal.projet_gestionautoplanningsecure.repository.AbsenceRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeAbsenceRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleAssignmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class PlanningQueryService {

    private final ScheduleAssignmentRepository assignmentRepository;
    private final EmployeeAbsenceRepository absenceRepository;

    /* ---------- 1. planning d’un EMPLOYÉ ---------- */
    public Map<LocalDate, List<AssignmentDTO>> getEmployeePlanning(Long employeeId, int month, int year) {
        LocalDate first = YearMonth.of(year, month).atDay(1);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());

        // Étape 1 : Créer une map vide
        Map<LocalDate, List<AssignmentDTO>> calendar = new TreeMap<>();

        // Étape 2 : Charger les absences
        List<EmployeeAbsence> absences = absenceRepository
                .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employeeId, last, first);

        // Générer une map des absences par jour
        Set<LocalDate> joursAbsents = new HashSet<>();
        for (EmployeeAbsence abs : absences) {
            LocalDate d = abs.getStartDate();
            while (!d.isAfter(abs.getEndDate())) {
                joursAbsents.add(d);
                calendar.computeIfAbsent(d, x -> new ArrayList<>())
                        .add(AssignmentDTO.fromEmployeeAbsence(abs, d));
                d = d.plusDays(1);
            }
        }

        // Étape 3 : Ajouter les vacations seulement si pas absent
        List<ScheduleAssignment> assignments = assignmentRepository
                .findByEmployeeIdAndDateBetween(employeeId, first, last);

        for (ScheduleAssignment a : assignments) {
            LocalDate date = a.getDate();
            if (!joursAbsents.contains(date)) {
                calendar.computeIfAbsent(date, d -> new ArrayList<>())
                        .add(AssignmentDTO.of(a));
            }
        }

        return calendar;
    }




    /* ---------- 2. planning d’un SITE (DTO complet) ---------- */
    public SitePlanningDTO getSitePlanning(Long siteId, int month, int year) {

        List<ScheduleAssignment> list =
                assignmentRepository.findBySiteMonthYear(siteId, month, year);

        Map<LocalDate, List<EmployeeShiftDTO>> calendar =
                list.stream()
                        .collect(Collectors.groupingBy(
                                ScheduleAssignment::getDate,
                                Collectors.mapping(this::toDto, toList())
                        ));

        String siteName = list.isEmpty()
                ? null
                : list.get(0).getSchedule().getSite().getName();

        return SitePlanningDTO.builder()
                .siteId(siteId)
                .siteName(siteName)
                .month(month)
                .year(year)
                .calendar(calendar)
                .build();
    }


    private EmployeeShiftDTO toDto(ScheduleAssignment a) {
        return EmployeeShiftDTO.builder()
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFirstName()
                        + " " + a.getEmployee().getLastName())
                .agentType(a.getAgentType().name())      // Enum → String
                .shiftLabel(a.getShift())                // "MATIN", …
                .startTime(a.getStartTime().toString())  // "08:00"
                .endTime(a.getEndTime().toString())      // "16:00"
                .build();
    }

    public Long findScheduleIdForEmployee(Long employeeId, int month, int year) {
        List<ScheduleAssignment> assignments =
                assignmentRepository.findByEmployeeAndScheduleMonthAndYear(employeeId, month, year);

        return assignments.isEmpty() ? null : assignments.get(0).getSchedule().getId();
    }


}
