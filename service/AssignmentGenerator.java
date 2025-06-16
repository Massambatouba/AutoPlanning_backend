package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleAssignmentRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.WeeklyScheduleRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j

@Service
@RequiredArgsConstructor
public class AssignmentGenerator {

    private final ScheduleRepository scheduleRepo;
    private final EmployeeRepository employeeRepo;
    private final WeeklyScheduleRuleRepository weeklyRuleRepo;
    private final ScheduleAssignmentRepository assignRepo;

    @Transactional
    public void generateForSchedule(Long scheduleId) {

        Schedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        Site site = schedule.getSite();
        YearMonth ym = YearMonth.of(schedule.getYear(), schedule.getMonth());
        int daysInMonth = ym.lengthOfMonth();

        List<WeeklyScheduleRule> rules = weeklyRuleRepo.findAllBySiteId(site.getId());

        if (rules.isEmpty()) throw new IllegalStateException("No weekly rules");

        List<ScheduleAssignment> toSave = new ArrayList<>();

        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = ym.atDay(d);
            DayOfWeek dow  = date.getDayOfWeek();

            // toutes les règles de ce jour (on peut en avoir plusieurs)
            rules.stream()
                    .filter(r -> r.getDayOfWeek() == dow)
                    .forEach(rule -> rule.getAgents().forEach(agentRule -> {

                        // 1. filtre par type d’agent
                        List<Employee> pool = employeeRepo
                                .findBySiteAndIsActiveTrueAndAgentTypesContaining(site, agentRule.getAgentType());

                        // 2. retrait des employés déjà planifiés sur ce créneau
                        pool = pool.stream()
                                .filter(emp -> isAvailable(emp, date,
                                        agentRule.getStartTime(),
                                        agentRule.getEndTime()))
                                .collect(Collectors.toList());

                        if (pool.size() < agentRule.getRequiredCount()) {
                            // Pas assez d’effectif, on log mais on continue
                            log.warn("Pas assez d’{} pour {} {}", agentRule.getAgentType(), date, site.getName());
                            return;
                        }

                        // 3. sélect° (round‑robin simple)
                        Collections.shuffle(pool);
                        pool.subList(0, agentRule.getRequiredCount()).forEach(emp -> {
                            ScheduleAssignment sa = ScheduleAssignment.builder()
                                    .schedule(schedule)
                                    .employee(emp)
                                    .date(date)
                                    .startTime(agentRule.getStartTime())
                                    .endTime(agentRule.getEndTime())
                                    .duration((int) Duration.between(agentRule.getStartTime(),
                                            agentRule.getEndTime()).toMinutes())
                                    .notes(agentRule.getNotes())
                                    .agentType(agentRule.getAgentType())
                                    .status(ScheduleAssignment.AssignmentStatus.PENDING)
                                    .build();
                            toSave.add(sa);
                        });
                    }));
        }

        assignRepo.saveAll(toSave);
    }

    /** true si l'employee n’a aucune assignation chevauchant ce créneau */
    private boolean isAvailable(Employee emp, LocalDate date, LocalTime start, LocalTime end) {
        return assignRepo.findByEmployeeId(emp.getId()).stream()
                .noneMatch(a -> a.getDate().equals(date)
                        && !(a.getEndTime().isBefore(start) || a.getStartTime().isAfter(end)));
    }
}
