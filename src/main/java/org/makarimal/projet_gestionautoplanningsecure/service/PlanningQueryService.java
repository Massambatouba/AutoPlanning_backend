package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAbsence;
import org.makarimal.projet_gestionautoplanningsecure.model.ScheduleAssignment;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeAbsenceRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleAssignmentRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class PlanningQueryService {

    private final ScheduleAssignmentRepository assignmentRepository;
    private final EmployeeAbsenceRepository    absenceRepository;
    private final SiteRepository               siteRepository;

    /* ───────── 1. Planning personnel ───────── */
    public Map<LocalDate, List<AssignmentDTO>>
    getEmployeePlanning(Long employeeId, int month, int year) {

        LocalDate first = YearMonth.of(year, month).atDay(1);
        LocalDate last  = first.withDayOfMonth(first.lengthOfMonth());

        Map<LocalDate, List<AssignmentDTO>> calendar = new TreeMap<>();

        /* Absences */
        List<EmployeeAbsence> absences =
                absenceRepository.findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employeeId, last, first);

        Set<LocalDate> joursAbsents = new HashSet<>();
        for (EmployeeAbsence abs : absences) {
            for (LocalDate d = abs.getStartDate();
                 !d.isAfter(abs.getEndDate());
                 d = d.plusDays(1)) {

                joursAbsents.add(d);

                // on évite les doublons : si déjà une absence ce jour-là, on ne rajoute pas
                boolean already = calendar.getOrDefault(d, List.of())
                        .stream()
                        .anyMatch(AssignmentDTO::isAbsence);
                if (!already) {
                    calendar.computeIfAbsent(d, x -> new ArrayList<>())
                            .add(AssignmentDTO.fromEmployeeAbsence(abs, d));
                }
            }
        }

        /* Vacations, seulement si pas absent */
        List<ScheduleAssignment> assigns =
                assignmentRepository.findByEmployeeIdAndDateBetween(employeeId, first, last);

        for (ScheduleAssignment a : assigns) {
            LocalDate d = a.getDate();
            if (!joursAbsents.contains(d)) {
                calendar.computeIfAbsent(d, x -> new ArrayList<>())
                        .add(AssignmentDTO.of(a));
            }
        }
        return calendar;
    }

    /* ───────── 2. Planning d’un site ───────── */
    public sitePlanningDTO getSitePlanning(Long siteId, int month, int year) {

        /* ①  on charge TOUTES les vacations du site pour le mois */
        List<ScheduleAssignment> vacations =
                assignmentRepository.findBySiteMonthYear(siteId, month, year);

        /* ②  on constitue, en une seule requête, l’ensemble des jours d’absence */
        LocalDate first = YearMonth.of(year, month).atDay(1);
        LocalDate last  = first.withDayOfMonth(first.lengthOfMonth());

        /* tous les salariés trouvés dans le planning */
        Set<Long> empIds = vacations.stream()
                .map(a -> a.getEmployee().getId())
                .collect(Collectors.toSet());

        /* ↓ récupère toutes les absences couvrant le mois en cours */
        List<EmployeeAbsence> absences =
                absenceRepository.findByEmployeeIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        empIds, last, first);

        /* on met chaque couple (empId, date) dans un Set pour lookup O(1) */
        Set<String> absentKey = new HashSet<>();
        for (EmployeeAbsence abs : absences) {
            for (LocalDate d = abs.getStartDate();
                 !d.isAfter(abs.getEndDate());
                 d = d.plusDays(1)) {

                absentKey.add(abs.getEmployee().getId() + "_" + d);   // ex. "42_2025-06-02"
            }
        }

        /* ③  filtrage définitif : on garde la vacation seulement si l’agent n’est pas absent ce jour-là */
        List<ScheduleAssignment> filtered = vacations.stream()
                .filter(a -> !absentKey.contains(a.getEmployee().getId() + "_" + a.getDate()))
                .toList();

        /* ---------- calendrier ---------- */
        Map<LocalDate, List<EmployeeShiftDTO>> calendar = filtered.stream()
                .collect(Collectors.groupingBy(
                        ScheduleAssignment::getDate,
                        Collectors.mapping(this::toDto, toList()))
                );

        /* ---------- employés actifs ---------- */
        List<EmployeePlanningDTO> employees = siteRepository.findById(siteId)
                .orElseThrow()
                .getEmployees().stream()
                .filter(Employee::isActive)
                .map(e -> new EmployeePlanningDTO(
                        e.getId(),
                        e.getFirstName() + " " + e.getLastName()))
                .toList();

        /* ---------- nom du site ---------- */
        String siteName = filtered.isEmpty()
                ? null
                : filtered.get(0).getSite().getName();

        return sitePlanningDTO.builder()
                .siteId(siteId)
                .siteName(siteName)
                .month(month)
                .year(year)
                .calendar(calendar)
                .employees(employees)
                .build();
    }



    /* ───────── convertisseur utilitaire ───────── */
    private EmployeeShiftDTO toDto(ScheduleAssignment a) {
        return EmployeeShiftDTO.builder()
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFirstName()
                        + " " + a.getEmployee().getLastName())
                .agentType(a.getAgentType().name())
                .shiftLabel(a.getShift())
                .startTime(a.getStartTime().toString())
                .endTime(a.getEndTime().toString())
                .build();
    }


    /* ───────── aide : retrouver l’ID d’un planning employé ───────── */
    public Long findScheduleIdForEmployee(Long employeeId, int month, int year) {
        return assignmentRepository
                .findByEmployeeAndScheduleMonthAndYear(employeeId, month, year)
                .stream()
                .findFirst()
                .map(a -> a.getSchedule().getId())
                .orElse(null);
    }
}
