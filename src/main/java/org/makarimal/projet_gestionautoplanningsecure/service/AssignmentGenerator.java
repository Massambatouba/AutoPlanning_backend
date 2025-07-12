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
        YearMonth ym = YearMonth.of(schedule.getYear(), schedule.getMonth());
        int daysInMonth = ym.lengthOfMonth();

        List<WeeklyScheduleRule> rules = weeklyRuleRepo.findAllBySiteId(site.getId());

        if (rules.isEmpty()) throw new IllegalStateException("No weekly rules");

        // ‼️ Repère des affectations déjà choisies mais pas encore persistées
        //    clé = date, valeur = ids des employés déjà retenus ce jour-là
        Map<LocalDate, Set<Long>> picked = new HashMap<>();

        List<ScheduleAssignment> toSave = new ArrayList<>();

        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = ym.atDay(d);
            DayOfWeek dow  = date.getDayOfWeek();
            picked.putIfAbsent(date, new HashSet<>());      // init

            // toutes les règles de ce jour (on peut en avoir plusieurs)
            rules.stream()
                    .filter(r -> r.getDayOfWeek() == dow)
                    .forEach(rule -> rule.getAgents().forEach(agentRule -> {

                        // 1. filtre par type d’agent
                        List<Employee> pool = employeeRepo
                                .findBySiteAndIsActiveTrueAndAgentTypesContaining(site, agentRule.getAgentType());

                        // 2. retrait des employés déjà planifiés sur ce créneau
                        pool = pool.stream()
                                .filter(emp ->
                                        isAvailable(emp, date,
                                                agentRule.getStartTime(),
                                                agentRule.getEndTime())
                                                /* déjà choisi pour cette journée ? */
                                                && !picked.get(date).contains(emp.getId())
                                                /* dépassement d’heures / préférences ? */
                                                && respectsPreferences(emp, date,
                                                agentRule.getStartTime(),
                                                agentRule.getEndTime())
                                )
                                .collect(Collectors.toList());


                        if (pool.size() < agentRule.getRequiredCount()) {
                            // Pas assez d’effectif, on log mais on continue
                            log.warn("Pas assez d’{} pour {} {}", agentRule.getAgentType(), date, site.getName());
                            return;
                        }

                        // 3. sélect° (round‑robin simple)
                        Collections.shuffle(pool);
                        pool.subList(0, agentRule.getRequiredCount()).forEach(emp -> {
                            picked.get(date).add(emp.getId());
                            /* nouveau filtre anti-doublon  */
                            if (assignRepo.existsByEmployeeIdAndDate(emp.getId(), date)) {
                                return;                    // une vacation pour ce jour existe déjà → on ignore
                            }
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
