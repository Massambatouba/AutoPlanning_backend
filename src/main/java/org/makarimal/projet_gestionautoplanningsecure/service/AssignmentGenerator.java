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
import java.util.*;
import java.util.stream.Collectors;

import static org.makarimal.projet_gestionautoplanningsecure.service.ScheduleService.buildInterval;

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

        // 1) Déterminer la plage de dates à couvrir selon periodType
        LocalDate start;
        LocalDate end;
        if (schedule.getPeriodType() == Schedule.PeriodType.RANGE) {
            if (schedule.getStartDate() == null || schedule.getEndDate() == null) {
                throw new IllegalStateException("RANGE schedule requires startDate & endDate");
            }
            start = schedule.getStartDate();
            end   = schedule.getEndDate();
        } else {
            // par défaut: planning mensuel
            Integer y = schedule.getYear();
            Integer m = schedule.getMonth();
            if (y == null || m == null) {
                throw new IllegalStateException("MONTH schedule requires year & month");
            }
            YearMonth ym = YearMonth.of(y, m);
            start = ym.atDay(1);
            end   = ym.atEndOfMonth();
        }

        List<WeeklyScheduleRule> rules = weeklyRuleRepo.findAllBySiteId(site.getId());
        if (rules.isEmpty()) throw new IllegalStateException("No weekly rules");

        // (optionnel) si tu veux regénérer proprement la période :
        // assignRepo.deleteByScheduleIdAndDateBetween(scheduleId, start, end);

        // Suivi local des personnes déjà retenues par jour
        Map<LocalDate, Set<Long>> picked = new HashMap<>();
        List<ScheduleAssignment> toSave = new ArrayList<>();

        // 2) Boucle du start au end (inclus)
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            final LocalDate day = d; // ← copie finale pour les lambdas

            DayOfWeek dow = day.getDayOfWeek();
            picked.putIfAbsent(day, new HashSet<>());

            rules.stream()
                    .filter(r -> r.getDayOfWeek() == dow)
                    .forEach(rule -> rule.getAgents().forEach(agentRule -> {

                        // 1) pool d’employés éligibles
                        List<Employee> pool = employeeRepo.findBySiteAndIsActiveTrueAndAgentTypesContaining(
                                site, agentRule.getAgentType());

                        // 2) filtres (dispo, déjà pris ce jour, préférences)
                        pool = pool.stream()
                                .filter(emp ->
                                        isAvailable(emp, day, agentRule.getStartTime(), agentRule.getEndTime()) &&
                                                !picked.get(day).contains(emp.getId()) &&
                                                respectsPreferences(emp, day, agentRule.getStartTime(), agentRule.getEndTime())
                                )
                                .collect(Collectors.toList());

                        if (pool.size() < agentRule.getRequiredCount()) {
                            log.warn("Pas assez d’{} pour {} {}", agentRule.getAgentType(), day, site.getName());
                            return;
                        }

                        // 3) sélection
                        Collections.shuffle(pool);
                        pool.subList(0, agentRule.getRequiredCount()).forEach(emp -> {
                            picked.get(day).add(emp.getId());

                            if (assignRepo.existsByEmployeeIdAndDate(emp.getId(), day)) {
                                return;
                            }

                            int durMin = computeDuration(agentRule.getStartTime(), agentRule.getEndTime());

                            ScheduleAssignment sa = ScheduleAssignment.builder()
                                    .schedule(schedule)
                                    .employee(emp)
                                    .date(day) // ← utilise la copie finale
                                    .startTime(agentRule.getStartTime())
                                    .endTime(agentRule.getEndTime())
                                    .duration(durMin)
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

    private int computeDuration(LocalTime start, LocalTime end) {
        int mins = (int) Duration.between(start, end).toMinutes();
        if (mins <= 0) mins += 24 * 60;
        return mins;
}


        /** true si l'employee n’a aucune assignation chevauchant ce créneau */
   /* private boolean isAvailable(Employee emp, LocalDate date, LocalTime start, LocalTime end) {
        return assignRepo.findByEmployeeId(emp.getId()).stream()
                .noneMatch(a -> a.getDate().equals(date)
                        && !(a.getEndTime().isBefore(start) || a.getStartTime().isAfter(end)));
    }

    */
    private boolean isAvailable(Employee emp,
                                LocalDate date,
                                LocalTime start,
                                LocalTime end) {

        LocalDateTime[] n = buildInterval(date, start, end);
        LocalDateTime nStart = n[0];
        LocalDateTime nEnd   = n[1];

        return assignRepo.findByEmployeeId(emp.getId()).stream()
                .noneMatch(a -> {
                    LocalDateTime[] o = buildInterval(
                            a.getDate(), a.getStartTime(), a.getEndTime());
                    return nStart.isBefore(o[1]) && nEnd.isAfter(o[0]);
                });
    }

    /** Durée d’une vacation en minutes (gère le passage de minuit) */
    private int durationMin(ScheduleAssignment a) {
        LocalTime start = a.getStartTime();
        LocalTime end   = a.getEndTime();

        int mins = (int) Duration.between(start, end).toMinutes();
        if (mins <= 0) mins += 24 * 60;   // vacation qui chevauche 00:00
        return mins;
    }


    /** minutes travaillées par l’employé entre 00:00 lundi et 23:59 dimanche */
    private int minutesWorkedThisWeek(Long empId, LocalDate date) {

        LocalDate monday   = date.with(DayOfWeek.MONDAY);
        LocalDate sunday   = monday.plusDays(6);

        return assignRepo                         // vacations déjà enregistrées
                .findByEmployeeIdAndDateBetween(empId, monday, sunday)
                .stream()
                .mapToInt(this::durationMin)
                .sum();
    }

    /** true si (date/heure) respecte les préférences de l’employé */
    private boolean respectsPreferences(Employee emp,
                                        LocalDate date,
                                        LocalTime start,
                                        LocalTime end) {

        EmployeePreference p = emp.getPreference();
        if (p == null) return true;                       // rien de renseigné

        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isNight   =  start.isAfter(LocalTime.of(22,0))
                || end  .isBefore(LocalTime.of(6 ,0));

        /* disponibilité jour / nuit / semaine */
        if (isWeekend && !p.isCanWorkWeekends()) return false;
        if (!isWeekend && !p.isCanWorkWeeks())    return false;
        if (isNight   && !p.isCanWorkNights())    return false;

        /* mini / maxi heures par jour */
        int durMin = (int) Duration.between(start, end).toMinutes();
        if (durMin <  p.getMinHoursPerDay()*60) return false;
        if (durMin >  p.getMaxHoursPerDay()*60) return false;

        /* mini / maxi heures par semaine */
        int after   = minutesWorkedThisWeek(emp.getId(), date) + durMin;
        int minWeek = p.getMinHoursPerWeek() == null ? 0 : p.getMinHoursPerWeek()*60;
        int maxWeek = p.getMaxHoursPerWeek() == null ? Integer.MAX_VALUE
                : p.getMaxHoursPerWeek()*60;
        return after <= maxWeek && after >= minWeek;
    }




}
