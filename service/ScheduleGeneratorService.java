package org.makarimal.projet_gestionautoplanningsecure.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.ScheduleRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteResponse;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.AbsenceRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleGeneratorService {
    @Autowired
    private final ScheduleService scheduleService;
    @Autowired
    private final SiteService siteService;
    @Autowired
    private final EmployeeService employeeService;
    @Autowired
    private final SiteRuleService siteRuleService;
    @Autowired
    private final EmployeeAvailabilityService availabilityService;
    @Autowired
    private final ScheduleAssignmentRepository assignmentRepository;
    @Autowired
    private final AbsenceRepository absenceRepository;

    @Transactional
    public Schedule generateSchedule(Long companyId, Long siteId, int month, int year) {
        // Vérifier que le site existe et appartient à l'entreprise
        SiteResponse site = siteService.getSite(companyId, siteId);

        // Récupérer les règles hebdomadaires et les vacations du site
        List<WeeklyScheduleRule> siteRules = siteRuleService.getWeeklyScheduleRules(companyId, siteId);
        List<SiteShift> shifts = siteRuleService.getSiteShifts(companyId, siteId);

        if (siteRules.isEmpty()) {
            throw new IllegalStateException("Aucune règle ou vacation définie pour le site");
        }

        // Créer le planning mensuel
        Schedule schedule = scheduleService.createOrRefresh(companyId, new ScheduleRequest(
                site.getName() + " - " + month + "/" + year,
                siteId,
                month,
                year
        ));

        // Générer les affectations pour chaque jour du mois
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek currentDay = date.getDayOfWeek();

            // Récupérer la règle de la semaine pour le jour en cours
            Optional<WeeklyScheduleRule> ruleOpt = siteRules.stream()
                    .filter(rule -> rule.getDayOfWeek() == currentDay)
                    .findFirst();

            if (ruleOpt.isEmpty()) continue; // Aucun planning pour ce jour

            WeeklyScheduleRule ruleForDay = ruleOpt.get();

            // Filtrer les employés éligibles pour ce jour
            List<Employee> eligibleEmployees = employeeService.getActiveEmployeesForSite(siteId).stream()
                    .filter(Employee::isActive)
                    .filter(e -> hasRequiredSkills(e, ruleForDay.getRequiredSkills()))
                    .filter(e -> e.getMaxHoursPerWeek() >= ruleForDay.getMinEmployees() * 8)
                    .collect(Collectors.toList());

            /* ---------- TRACE À AJOUTER ICI ------------------------------ */
            log.debug("Day {}, règle {}, employés éligibles = {}",
                    date, ruleForDay.getDayOfWeek(), eligibleEmployees.size());
            /* ------------------------------------------------------------- */

            if (eligibleEmployees.isEmpty()) {
                System.out.println("Aucun employé éligible pour le " + currentDay);
                continue;
            }

            // Générer les assignations pour ce jour
            generateDailyAssignments(schedule, date, shifts, eligibleEmployees, ruleForDay);
        }

        // Mettre à jour le taux de complétion
        updateCompletionRate(schedule);

        return schedule;
    }


    private void generateDailyAssignments(
            Schedule schedule,
            LocalDate date,
            List<SiteShift> shifts,
            List<Employee> employees,
            WeeklyScheduleRule siteRule
    ) {
        for (SiteShift shift : shifts) {
            List<Employee> availableEmployees = findAvailableEmployees(
                    employees,
                    date,
                    shift.getStartTime(),
                    shift.getEndTime(),
                    shift.getMinExperience(),
                    shift.getRequiredSkills()
            );

            for (int i = 0; i < shift.getRequiredEmployees() && i < availableEmployees.size(); i++) {
                Employee employee = availableEmployees.get(i);

                ScheduleAssignment assignment = ScheduleAssignment.builder()
                        .schedule(schedule)
                        .employee(employee)
                        .date(date)
                        .startTime(shift.getStartTime())
                        .endTime(shift.getEndTime())
                        .duration(calculateDuration(shift.getStartTime(), shift.getEndTime()))
                        .status(ScheduleAssignment.AssignmentStatus.PENDING)
                        .build();

                // Lier au planning
                schedule.getAssignments().add(assignment);

                assignmentRepository.save(assignment);
            }
        }
    }


    private List<Employee> findAvailableEmployees(
            List<Employee> employees,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int minExperience,
            List<String> requiredSkills
    ) {
        List<Employee> availableEmployees = employees.stream()
                .filter(e -> isEmployeeAvailable(e, date, startTime, endTime))
                .filter(e -> !absenceRepository.existsByEmployeeIdAndDate(e.getId(), date))
                .filter(e -> hasRequiredSkills(e, requiredSkills))
                .sorted(Comparator
                        .comparing((Employee e) -> getEmployeeWorkload(e, date))
                        .thenComparing(e -> e.getSkillSets().size(), Comparator.reverseOrder())
                )
                .toList();

        /* ---------- TRACE À AJOUTER ICI ------------------------------ */
        log.debug("Shift {}–{}, dispo après filtres = {}",
                startTime, endTime, availableEmployees.size());
        /* ------------------------------------------------------------- */
        return availableEmployees;
    }

    private boolean isEmployeeAvailable(
            Employee employee,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        // Vérifier les disponibilités de l'employé
        List<EmployeeAvailability> availabilities = availabilityService.getAvailabilities(
                employee.getCompany().getId(),
                employee.getId()
        );

        boolean hasAvailability = availabilities.stream()
                .anyMatch(a -> a.getDayOfWeek() == date.getDayOfWeek() &&
                        !a.getStartTime().isAfter(startTime) &&
                        !a.getEndTime().isBefore(endTime));

        if (!hasAvailability) {
            return false;
        }

        // Vérifier les affectations existantes
        List<ScheduleAssignment> existingAssignments = assignmentRepository.findByEmployeeId(employee.getId());

        // Vérifier les chevauchements
        boolean hasConflict = existingAssignments.stream()
                .anyMatch(a -> a.getDate().equals(date) &&
                        !(a.getEndTime().isBefore(startTime) || a.getStartTime().isAfter(endTime)));

        if (hasConflict) {
            return false;
        }

        // Vérifier les heures max par jour
        int dailyHours = existingAssignments.stream()
                .filter(a -> a.getDate().equals(date))
                .mapToInt(ScheduleAssignment::getDuration)
                .sum();

        EmployeePreference preferences = availabilityService.getPreferences(
                employee.getCompany().getId(),
                employee.getId()
        );

        return (dailyHours + calculateDuration(startTime, endTime)) <= preferences.getMaxHoursPerDay();
    }

    private int getEmployeeWorkload(Employee employee, LocalDate date) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        return assignmentRepository.findByEmployeeId(employee.getId()).stream()
                .filter(a -> !a.getDate().isBefore(weekStart) && !a.getDate().isAfter(weekEnd))
                .mapToInt(ScheduleAssignment::getDuration)
                .sum();
    }

    private boolean hasRequiredSkills(Employee employee, List<String> requiredSkills) {
        return employee.getSkillSets().containsAll(requiredSkills);
    }

    private boolean isWorkingDay(DayOfWeek dayOfWeek, WeeklyScheduleRule rule) {
        return rule.getDayOfWeek().equals(dayOfWeek);
    }

    private int calculateDuration(LocalTime start, LocalTime end) {
        return (int) java.time.Duration.between(start, end).toMinutes();
    }

    private void updateCompletionRate(Schedule schedule) {
        List<ScheduleAssignment> assignments = assignmentRepository.findByScheduleId(schedule.getId());

        if (assignments.isEmpty()) {
            schedule.setCompletionRate(0);
        } else {
            long confirmedCount = assignments.stream()
                    .filter(a -> a.getStatus() == ScheduleAssignment.AssignmentStatus.CONFIRMED)
                    .count();

            int completionRate = (int) ((confirmedCount * 100.0) / assignments.size());
            schedule.setCompletionRate(completionRate);
        }
    }
}
