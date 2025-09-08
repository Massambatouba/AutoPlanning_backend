package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.ScheduleRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteResponse;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeAbsenceRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleAssignmentRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteWeeklyExceptionRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleGeneratorService {

    private final SiteWeeklyExceptionRepository weeklyExcRepo;
    private final ScheduleService scheduleService;
    private final SiteService siteService;
    private final EmployeeService employeeService;
    private final SiteRuleService siteRuleService;
    private final EmployeeAvailabilityService availabilityService;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final EmployeeAbsenceRepository absenceRepository;

    /* =========================================================
     * ===============  MOTEUR CLASSIQUE (existant) ============
     * ========================================================= */
    @Transactional
    public Schedule generateSchedule(Long companyId, Long siteId, int month, int year) throws AccessDeniedException {
        SiteResponse site = siteService.getSite(companyId, siteId);
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<WeeklyScheduleRule> siteRules = siteRuleService.getWeeklyScheduleRules(companyId, siteId);
        List<SiteShift> baseShifts = siteRuleService.getSiteShifts(companyId, siteId);

        if (siteRules == null || siteRules.isEmpty()) {
            throw new IllegalStateException("Aucune règle/vacation définie pour le site");
        }

        ScheduleRequest req = ScheduleRequest.builder()
                .name(site.getName() + " - " + month + "/" + year)
                .siteId(siteId)
                .periodType(Schedule.PeriodType.MONTH)
                .month(month)
                .year(year)
                .build();

        Schedule schedule = scheduleService.createOrRefresh(me, companyId, req);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate d0 = ym.atDay(1);
        LocalDate d1 = ym.atEndOfMonth();

        for (LocalDate date = d0; !date.isAfter(d1); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();

            Optional<WeeklyScheduleRule> ruleOpt = siteRules.stream()
                    .filter(r -> r.getDayOfWeek() == dow)
                    .findFirst();
            if (ruleOpt.isEmpty()) continue;
            WeeklyScheduleRule ruleForDay = ruleOpt.get();

            // Shifts effectifs après exceptions
            List<SiteShift> dayShifts = computeEffectiveShifts(siteId, date, baseShifts);
            if (dayShifts.isEmpty()) {
                log.info("[GEN] {} : aucun shift (fermé ou tout masqué par exceptions)", date);
                continue;
            }

            // Pool = employés rattachés au site et actifs
            List<Employee> eligibleEmployees = employeeService.getActiveEmployeesForSite(siteId).stream()
                    .filter(Employee::isActive)
                    .collect(Collectors.toList());

            log.debug("[GEN] {} rule={}, pool={}", date, ruleForDay.getDayOfWeek(), eligibleEmployees.size());
            if (eligibleEmployees.isEmpty()) continue;

            generateDailyAssignments(schedule, date, dayShifts, eligibleEmployees);
        }

        updateCompletionRate(schedule);
        return schedule;
    }

    private void generateDailyAssignments(
            Schedule schedule,
            LocalDate date,
            List<SiteShift> shifts,
            List<Employee> employees
    ) {
        for (SiteShift shift : safe(shifts)) {
            List<Employee> available = findAvailableEmployees(
                    employees,
                    date,
                    shift.getStartTime(),
                    shift.getEndTime(),
                    shift.getRequiredSkills()
            );

            int needed = Math.max(0, nvl(shift.getRequiredEmployees(), 1));
            for (int i = 0; i < needed && i < available.size(); i++) {
                Employee e = available.get(i);
                AgentType at = pickAgentType(e, null);

                ScheduleAssignment a = ScheduleAssignment.builder()
                        .schedule(schedule)
                        .employee(e)
                        .date(date)
                        .startTime(shift.getStartTime())
                        .endTime(shift.getEndTime())
                        .duration(calculateDuration(shift.getStartTime(), shift.getEndTime()))
                        .status(ScheduleAssignment.AssignmentStatus.PENDING)
                        .agentType(at) // <<-- OBLIGATOIRE
                        .shift(buildShiftLabel(at, shift.getStartTime(), shift.getEndTime(), shift.getName())) // optionnel
                        .build();

                schedule.getAssignments().add(a);
                assignmentRepository.save(a);
            }

        }
    }

    private List<Employee> findAvailableEmployees(
            List<Employee> pool,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            List<String> requiredSkills
    ) {
        return safe(pool).stream()
                .filter(e -> isEmployeeAvailable(e, date, start, end))
                .filter(e -> !absenceRepository.existsByEmployeeIdAndDate(e.getId(), date))
                .filter(e -> hasRequiredSkills(e, requiredSkills))
                .sorted(Comparator
                        .comparing((Employee e) -> getEmployeeWorkload(e, date))
                        .thenComparing(e -> sizeSafe(getSkills(e)), Comparator.reverseOrder())
                )
                .toList();
    }

    /* =========================================================
     * ====================  MOTEUR SMART V2  ==================
     * ========================================================= */
    @Transactional
    public Schedule generateScheduleV2(Long companyId, Long siteId, int month, int year) throws AccessDeniedException {
        SiteResponse site = siteService.getSite(companyId, siteId);
        User me = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<WeeklyScheduleRule> siteRules = siteRuleService.getWeeklyScheduleRules(companyId, siteId);
        if (siteRules == null || siteRules.isEmpty()) {
            throw new IllegalStateException("Aucune règle hebdomadaire définie pour le site");
        }

        ScheduleRequest req = ScheduleRequest.builder()
                .name(site.getName() + " - " + month + "/" + year)
                .siteId(siteId)
                .periodType(Schedule.PeriodType.MONTH)
                .month(month)
                .year(year)
                .build();

        Schedule schedule = scheduleService.createOrRefresh(me, companyId, req);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate d0 = ym.atDay(1), d1 = ym.atEndOfMonth();

        for (LocalDate d = d0; !d.isAfter(d1); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();

            var ruleOpt = siteRules.stream().filter(r -> r.getDayOfWeek() == dow).findFirst();
            if (ruleOpt.isEmpty()) continue;
            var rule = ruleOpt.get();

            List<GenSpec> specs = buildSpecsForDayV2(siteId, d, rule);
            if (specs.isEmpty()) {
                log.info("[GEN V2] {} : fermé / rien à poser", d);
                continue;
            }

            List<Employee> pool = employeeService.getActiveEmployeesForSite(siteId).stream()
                    .filter(Employee::isActive)
                    .collect(Collectors.toList());
            if (pool.isEmpty()) continue;

            generateFromSpecs(schedule, d, specs, pool);
        }

        updateCompletionRate(schedule);
        return schedule;
    }

    /** Construit les créneaux du jour = weekly (agents) ± exceptions. */
    private List<GenSpec> buildSpecsForDayV2(Long siteId, LocalDate date, WeeklyScheduleRule ruleForDay) {
        DayOfWeek dow = date.getDayOfWeek();

        var excAll = weeklyExcRepo
                .findBySiteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(siteId, date, date)
                .stream()
                .filter(e -> matchesDaysOfWeek(e, dow))
                .toList();

        if (excAll.stream().anyMatch(e -> e.getType() == WeeklyExceptionType.CLOSE_DAY)) return List.of();

        var replaces = excAll.stream().filter(e -> e.getType() == WeeklyExceptionType.REPLACE_DAY).toList();
        List<GenSpec> out = new ArrayList<>();
        if (!replaces.isEmpty()) {
            for (var e : replaces) {
                out.add(new GenSpec(
                        e.getAgentType(),
                        e.getStartTime(),
                        e.getEndTime(),
                        nvl(e.getRequiredCount(), 1),
                        nvl(e.getMinExperience(), 0),
                        safe(e.getRequiredSkills())
                ));
            }
            return out;
        }

        if (ruleForDay.getAgents() != null) {
            for (AgentSchedule a : ruleForDay.getAgents()) {
                out.add(new GenSpec(
                        a.getAgentType(),
                        a.getStartTime(),
                        a.getEndTime(),
                        nvl(a.getRequiredCount(), 1),
                        0,
                        safe(ruleForDay.getRequiredSkills())
                ));
            }
        }

        List<GenSpec> adds = excAll.stream()
                .filter(e -> e.getType() == WeeklyExceptionType.ADD_SHIFT)
                .map(e -> new GenSpec(
                        e.getAgentType(),
                        e.getStartTime(),
                        e.getEndTime(),
                        nvl(e.getRequiredCount(), 1),
                        nvl(e.getMinExperience(), 0),
                        safe(e.getRequiredSkills())
                ))
                .collect(Collectors.toList());

        out.addAll(adds);

        var masks = excAll.stream().filter(e -> e.getType() == WeeklyExceptionType.MASK_SHIFT).toList();
        if (!masks.isEmpty()) {
            out = out.stream().filter(s -> {
                for (var m : masks) {
                    if (m.getAgentType() != null && m.getAgentType() != s.getAgentType()) continue;
                    if (m.getStartTime() != null && m.getEndTime() != null) {
                        if (!timeRangesOverlap(s.getStart(), s.getEnd(), m.getStartTime(), m.getEndTime())) continue;
                    }
                    return false; // masqué
                }
                return true;
            }).toList();
        }

        return out;
    }

    private void generateFromSpecs(
            Schedule schedule,
            LocalDate date,
            List<GenSpec> specs,
            List<Employee> pool
    ) {
        for (GenSpec spec : safe(specs)) {
            int need = Math.max(0, spec.getRequiredCount());
            for (int i = 0; i < need; i++) {
                Optional<Employee> candidate = safe(pool).stream()
                        .filter(e -> hasAgentType(e, spec.getAgentType()))
                        .filter(e -> isEmployeeAvailable(e, date, spec.getStart(), spec.getEnd()))
                        .filter(e -> !absenceRepository.existsByEmployeeIdAndDate(e.getId(), date))
                        .filter(e -> hasRequiredSkills(e, spec.getSkills()))
                        .filter(e -> canAssignMoreWeeklyMinutes(e, date, calculateDuration(spec.getStart(), spec.getEnd())))
                        .min(Comparator.comparing(e -> getEmployeeWorkload(e, date)));

                if (candidate.isEmpty()) {
                    log.warn("[GEN V2] {} {}-{} : aucun candidat",
                            spec.getAgentType(), spec.getStart(), spec.getEnd());
                    continue;
                }

                Employee chosen = candidate.get();
                AgentType at = pickAgentType(chosen, spec.getAgentType());

                ScheduleAssignment a = ScheduleAssignment.builder()
                        .schedule(schedule)
                        .employee(chosen)
                        .date(date)
                        .startTime(spec.getStart())
                        .endTime(spec.getEnd())
                        .duration(calculateDuration(spec.getStart(), spec.getEnd()))
                        .status(ScheduleAssignment.AssignmentStatus.PENDING)
                        .agentType(at)  // <<-- OBLIGATOIRE
                        .shift(buildShiftLabel(at, spec.getStart(), spec.getEnd(), null)) // optionnel mais pratique
                        .build();

                schedule.getAssignments().add(a);
                assignmentRepository.save(a);
            }
        }
    }

    /* =========================================================
     * =======================  CHECKS  ========================
     * ========================================================= */

    private boolean isEmployeeAvailable(Employee employee,
                                        LocalDate date,
                                        LocalTime startTime,
                                        LocalTime endTime) {
        // 1) Dispos structurées
        List<EmployeeAvailability> av = availabilityService.getAvailabilities(
                employee.getCompany().getId(), employee.getId());

        boolean okAvail = !safe(av).isEmpty() && safe(av).stream().anyMatch(a ->
                a.getDayOfWeek() == date.getDayOfWeek()
                        && !a.getStartTime().isAfter(startTime)
                        && !a.getEndTime().isBefore(endTime)
        );

        // 1.b) Fallback via préférences si aucune dispo en base
        if (!okAvail && safe(av).isEmpty()) {
            EmployeePreference pref = availabilityService.getPreferences(
                    employee.getCompany().getId(), employee.getId());
            okAvail = matchesByPreference(pref, date.getDayOfWeek(), startTime, endTime);
        }
        if (!okAvail) return false;

        // 2) Conflits (tous sites)
        List<ScheduleAssignment> existing = assignmentRepository.findByEmployeeId(employee.getId());
        boolean conflict = safe(existing).stream().anyMatch(a ->
                date.equals(a.getDate()) &&
                        !(a.getEndTime().isBefore(startTime) || a.getStartTime().isAfter(endTime))
        );
        if (conflict) return false;

        // 3) Plafond / jour
        int alreadyMin = safe(existing).stream()
                .filter(a -> date.equals(a.getDate()))
                .mapToInt(ScheduleAssignment::getDuration)
                .sum();

        EmployeePreference pref = availabilityService.getPreferences(
                employee.getCompany().getId(), employee.getId());

        int maxDayMin = 24 * 60; // défaut large si non défini
        if (pref != null && pref.getMaxHoursPerDay() != null && pref.getMaxHoursPerDay() > 0) {
            maxDayMin = pref.getMaxHoursPerDay() * 60;
        }

        int add = calculateDuration(startTime, endTime);
        return (alreadyMin + add) <= maxDayMin;
    }

    /** Fallback : interprète les préférences quand il n'y a pas de plages structurées. */
    private boolean matchesByPreference(EmployeePreference pref,
                                        DayOfWeek dow,
                                        LocalTime start,
                                        LocalTime end) {
        if (pref == null) return true; // pas d’info => on suppose dispo

        boolean weekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        // Week-end / semaine
        if (weekend && !pref.isCanWorkWeekends()) return false;
        if (!weekend && !pref.isCanWorkWeeks())   return false;

        // Sans préférence => OK
        if (pref.isNoPreference()) return true;

        // Définition simple jour/nuit — adapte si besoin
        LocalTime dayStart = LocalTime.of(6, 0);
        LocalTime dayEnd   = LocalTime.of(20, 0);
        boolean isDayShift = end.isAfter(start)
                ? (!start.isBefore(dayStart) && !end.isAfter(dayEnd))
                : (!start.isBefore(dayStart) && dayEnd.equals(LocalTime.MIDNIGHT)); // cas rare

        // Préférences jour/nuit (bloquantes)
        if (pref.isPrefersDay()   && !isDayShift) return false;
        if (pref.isPrefersNight() &&  isDayShift) return false;

        return true;
    }


    private boolean canAssignMoreWeeklyMinutes(Employee e, LocalDate date, int addMinutes) {
        int currentWeekMin = getEmployeeWorkload(e, date);

        Integer maxWeekHours = null;
        EmployeePreference pref = availabilityService.getPreferences(e.getCompany().getId(), e.getId());
        if (pref != null && pref.getMaxHoursPerWeek() != null && pref.getMaxHoursPerWeek() > 0) {
            maxWeekHours = pref.getMaxHoursPerWeek();
        } else if (e.getMaxHoursPerWeek() != null && e.getMaxHoursPerWeek() > 0) {
            maxWeekHours = e.getMaxHoursPerWeek();
        }

        if (maxWeekHours == null) return true; // pas de plafond
        return (currentWeekMin + addMinutes) <= maxWeekHours * 60;
    }

    private int getEmployeeWorkload(Employee employee, LocalDate date) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        return safe(assignmentRepository.findByEmployeeId(employee.getId())).stream()
                .filter(a -> !a.getDate().isBefore(weekStart) && !a.getDate().isAfter(weekEnd))
                .mapToInt(ScheduleAssignment::getDuration)
                .sum();
    }

    private boolean hasRequiredSkills(Employee employee, List<String> required) {
        if (required == null || required.isEmpty()) return true;
        Collection<String> empSkills = getSkills(employee);
        return empSkills.containsAll(required);
    }

    // Adapte ici si ton modèle s'appelle autrement (getSkills, getCompetences, etc.)
    private Collection<String> getSkills(Employee e) {
        return e.getSkillSets() == null ? List.of() : e.getSkillSets();
    }

    private boolean hasAgentType(Employee e, AgentType type) {
        if (type == null) return true; // pas de filtre
        Collection<AgentType> types = (e.getAgentTypes() == null) ? List.of() : e.getAgentTypes();
        return types.contains(type);
    }

    private int calculateDuration(LocalTime start, LocalTime end) {
        if (start == null || end == null) return 0;
        LocalDate base = LocalDate.of(2000, 1, 1);
        LocalDateTime s = LocalDateTime.of(base, start);
        LocalDateTime e = LocalDateTime.of(base, end);
        if (!end.isAfter(start)) e = e.plusDays(1);
        return (int) Duration.between(s, e).toMinutes();
    }

    private void updateCompletionRate(Schedule schedule) {
        List<ScheduleAssignment> assignments = assignmentRepository.findByScheduleId(schedule.getId());
        if (assignments == null || assignments.isEmpty()) {
            schedule.setCompletionRate(0);
            return;
        }
        long confirmed = assignments.stream()
                .filter(a -> a.getStatus() == ScheduleAssignment.AssignmentStatus.CONFIRMED)
                .count();
        int rate = (int) Math.round(confirmed * 100.0 / assignments.size());
        schedule.setCompletionRate(rate);
    }

    /* =========================================================
     * ==================  EXCEPTIONS (classique) ==============
     * ========================================================= */

    private List<SiteShift> computeEffectiveShifts(Long siteId, LocalDate date, List<SiteShift> baseShifts) {
        DayOfWeek dow = date.getDayOfWeek();

        List<SiteWeeklyException> excAll = weeklyExcRepo
                .findBySiteIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(siteId, date, date)
                .stream()
                .filter(e -> matchesDaysOfWeek(e, dow))
                .toList();

        boolean closed = excAll.stream().anyMatch(e -> e.getType() == WeeklyExceptionType.CLOSE_DAY);
        if (closed) return List.of();

        List<SiteWeeklyException> replaces = excAll.stream()
                .filter(e -> e.getType() == WeeklyExceptionType.REPLACE_DAY)
                .toList();

        List<SiteShift> out = new ArrayList<>();
        if (!replaces.isEmpty()) {
            for (var e : replaces) {
                out.add(SiteShift.builder()
                        .name((e.getAgentType() != null ? e.getAgentType().name() : "REPLACE") + " " +
                                String.valueOf(e.getStartTime()) + "-" + String.valueOf(e.getEndTime()))
                        .startTime(e.getStartTime())
                        .endTime(e.getEndTime())
                        .requiredEmployees(nvl(e.getRequiredCount(), 1))
                        .minExperience(nvl(e.getMinExperience(), 0))
                        .requiredSkills(safe(e.getRequiredSkills()))
                        .build());
            }
            return out;
        }

        out.addAll(safe(baseShifts));

        excAll.stream()
                .filter(e -> e.getType() == WeeklyExceptionType.ADD_SHIFT)
                .forEach(e -> out.add(SiteShift.builder()
                        .name((e.getAgentType() != null ? e.getAgentType().name() : "ADD") + " " +
                                String.valueOf(e.getStartTime()) + "-" + String.valueOf(e.getEndTime()))
                        .startTime(e.getStartTime())
                        .endTime(e.getEndTime())
                        .requiredEmployees(nvl(e.getRequiredCount(), 1))
                        .minExperience(nvl(e.getMinExperience(), 0))
                        .requiredSkills(safe(e.getRequiredSkills()))
                        .build()));

        List<SiteWeeklyException> masks = excAll.stream()
                .filter(e -> e.getType() == WeeklyExceptionType.MASK_SHIFT)
                .toList();

        if (!masks.isEmpty()) {
            out.removeIf(s -> {
                for (var m : masks) {
                    if (m.getAgentType() != null && s.getName() != null &&
                            !s.getName().toUpperCase().contains(m.getAgentType().name())) {
                        continue;
                    }
                    if (m.getStartTime() != null && m.getEndTime() != null) {
                        if (!timeRangesOverlap(s.getStartTime(), s.getEndTime(), m.getStartTime(), m.getEndTime())) {
                            continue;
                        }
                    }
                    return true; // masque appliqué
                }
                return false;
            });
        }
        return out;
    }

    private boolean matchesDaysOfWeek(SiteWeeklyException e, DayOfWeek dow) {
        return e.getDaysOfWeek() == null || e.getDaysOfWeek().isEmpty() || e.getDaysOfWeek().contains(dow);
    }

    private boolean timeRangesOverlap(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;
        var base = LocalDate.of(2000, 1, 1);
        var a0 = LocalDateTime.of(base, aStart);
        var a1 = LocalDateTime.of(base, aEnd);
        if (!aEnd.isAfter(aStart)) a1 = a1.plusDays(1);

        var b0 = LocalDateTime.of(base, bStart);
        var b1 = LocalDateTime.of(base, bEnd);
        if (!bEnd.isAfter(bStart)) b1 = b1.plusDays(1);

        return a0.isBefore(b1) && a1.isAfter(b0);
    }

    /* =========================================================
     * ==================  OUTILS DE SÉCURITÉ  =================
     * ========================================================= */

    private static <T> List<T> safe(List<T> in) {
        return in == null ? List.of() : in;
    }

    private static int nvl(Integer v, int def) {
        return v == null ? def : v;
    }

    private static int sizeSafe(Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    /** Remplace le record par une classe interne pour compatibilité Java 8/11. */
    private static final class GenSpec {
        private final AgentType agentType;
        private final LocalTime start;
        private final LocalTime end;
        private final int requiredCount;
        private final int minExp;
        private final List<String> skills;

        private GenSpec(AgentType agentType, LocalTime start, LocalTime end,
                        int requiredCount, int minExp, List<String> skills) {
            this.agentType = agentType;
            this.start = start;
            this.end = end;
            this.requiredCount = requiredCount;
            this.minExp = minExp;
            this.skills = skills == null ? List.of() : skills;
        }

        public AgentType getAgentType() { return agentType; }
        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }
        public int getRequiredCount() { return requiredCount; }
        public int getMinExp() { return minExp; }
        public List<String> getSkills() { return skills; }
    }

    private AgentType pickAgentType(Employee e, AgentType wanted) {
        if (wanted != null) return wanted;
        if (e.getAgentTypes() != null && !e.getAgentTypes().isEmpty()) {
            // on prend le premier type déclaré de l’employé
            return e.getAgentTypes().iterator().next();
        }
        // fallback "sûr" si rien n’est fourni (ajuste si tu préfères AgentType.ADS)
        return AgentType.values()[0];
    }

    private String buildShiftLabel(AgentType type, LocalTime start, LocalTime end, String nameIfAny) {
        if (nameIfAny != null && !nameIfAny.isBlank()) return nameIfAny;
        String base = (type != null ? type.name() + " " : "");
        return base + String.valueOf(start) + "-" + String.valueOf(end);
    }

}
