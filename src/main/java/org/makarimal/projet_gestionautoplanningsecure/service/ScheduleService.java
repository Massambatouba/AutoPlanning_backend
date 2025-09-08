package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final MailService mailService;
    private final PlanningPdfService planningPdfService;

    private final EmployeeAbsenceRepository absenceRepository;
    private final NotificationService  notificationService;
    private final UserRepository userRepository;

    /* ------------------------------------------------------------------
       Helpers
       ------------------------------------------------------------------ */

    private void assertCanManage(User actor, Long siteId) throws AccessDeniedException {
        User u = userRepository.findByIdWithSites(actor.getId())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));
        if (!u.canAccessSite(siteId)) {
            throw new AccessDeniedException("Vous n’avez pas le droit de gérer ce site");
        }
    }

    /** Construit un nom « safe » : si le client ne l’a pas fourni,
     *  on utilise :  « <Nom du site> - MM/YYYY ».
     */
    private String produceName(ScheduleRequest req, Site site) {
        if (req.getName() != null && !req.getName().isBlank()) {
            return req.getName().trim();
        }
        String base = (site != null && site.getName() != null) ? site.getName() : "Planning";

        var t = (req.getPeriodType() == null) ? Schedule.PeriodType.MONTH : req.getPeriodType();
        if (t == Schedule.PeriodType.RANGE && req.getStartDate() != null && req.getEndDate() != null) {
            return String.format("%s - %1$td/%1$tm/%1$tY → %2$td/%2$tm/%2$tY",
                    base, req.getStartDate(), req.getEndDate());
        }
        if (req.getMonth() != null && req.getYear() != null) {
            return String.format("%s - %02d/%04d", base, req.getMonth(), req.getYear());
        }
        return base;
    }

    /* ===============================================================
       CREATE  ▸ ou ▸  REFRESH (mensuel)
       =============================================================== */
    @Transactional
    public Schedule createOrRefresh(User actor, Long companyId, ScheduleRequest req) throws AccessDeniedException {

        assertCanManage(actor, req.getSiteId());

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        Site site = siteRepository.findById(req.getSiteId())
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found or not owned by company"));

        Schedule.PeriodType type = (req.getPeriodType() == null) ? Schedule.PeriodType.MONTH : req.getPeriodType();
        Schedule schedule;

        if (type == Schedule.PeriodType.MONTH) {
            // validations
            if (req.getMonth() == null || req.getYear() == null) {
                throw new IllegalArgumentException("year & month are required for MONTH");
            }

            // trouver existant
            schedule = scheduleRepository
                    .findBySiteIdAndPeriodTypeAndMonthAndYear(site.getId(), Schedule.PeriodType.MONTH, req.getMonth(), req.getYear())
                    .orElse(null);

            if (schedule != null) {
                // purge + reset
                assignmentRepository.deleteAll(assignmentRepository.findByScheduleId(schedule.getId()));
                schedule.setCompletionRate(0);
                schedule.setPublished(false);
                schedule.setSent(false);
                schedule.setSentAt(null);
                schedule.setName(produceName(req, site));
                schedule.setPeriodType(Schedule.PeriodType.MONTH);
                schedule.setStartDate(null);
                schedule.setEndDate(null);
            } else {
                schedule = Schedule.builder()
                        .company(company)
                        .site(site)
                        .periodType(Schedule.PeriodType.MONTH)
                        .name(produceName(req, site))
                        .month(req.getMonth())
                        .year(req.getYear())
                        .published(false)
                        .sent(false)
                        .completionRate(0)
                        .build();
            }
        } else { // RANGE
            // validations
            if (req.getStartDate() == null || req.getEndDate() == null) {
                throw new IllegalArgumentException("startDate & endDate are required for RANGE");
            }
            if (req.getEndDate().isBefore(req.getStartDate())) {
                throw new IllegalArgumentException("endDate must be >= startDate");
            }

            // (optionnel) empêcher chevauchement de plage
            List<Schedule> overlaps = scheduleRepository.findRangeOverlaps(site.getId(), req.getStartDate(), req.getEndDate());
            if (!overlaps.isEmpty()) {
                // tu peux être plus fin si tu acceptes des doublons exacts, etc.
                throw new IllegalStateException("Un planning (RANGE) chevauche déjà cette plage pour ce site.");
            }

            // trouver exact
            schedule = scheduleRepository
                    .findBySiteIdAndPeriodTypeAndStartDateAndEndDate(
                            site.getId(), Schedule.PeriodType.RANGE, req.getStartDate(), req.getEndDate())
                    .orElse(null);

            if (schedule != null) {
                assignmentRepository.deleteAll(assignmentRepository.findByScheduleId(schedule.getId()));
                schedule.setCompletionRate(0);
                schedule.setPublished(false);
                schedule.setSent(false);
                schedule.setSentAt(null);
                schedule.setName(produceName(req, site));
                schedule.setPeriodType(Schedule.PeriodType.RANGE);
                schedule.setMonth(null);
                schedule.setYear(null);
                schedule.setStartDate(req.getStartDate());
                schedule.setEndDate(req.getEndDate());
            } else {
                schedule = Schedule.builder()
                        .company(company)
                        .site(site)
                        .periodType(Schedule.PeriodType.RANGE)
                        .name(produceName(req, site))
                        .startDate(req.getStartDate())
                        .endDate(req.getEndDate())
                        .published(false)
                        .sent(false)
                        .completionRate(0)
                        .build();
            }
        }

        schedule = scheduleRepository.save(schedule);

        log.info("Schedule [{}] type={} saved (id={})",
                schedule.getName(), schedule.getPeriodType(), schedule.getId());

        return schedule;
    }


    @Transactional
    public Schedule updateSchedule(User actor, Long companyId, Long scheduleId, ScheduleRequest req)
            throws AccessDeniedException {

        Schedule s = getSchedule(companyId, scheduleId);

        // droits
        assertCanManage(actor, s.getSite().getId());
        if (req.getSiteId() != null) {
            assertCanManage(actor, req.getSiteId());
        }

        // interdictions (à adapter si besoin)
        if (s.isPublished() || s.isSent()) {
            throw new IllegalStateException("Impossible de modifier un planning publié ou déjà envoyé");
        }

        // site
        if (req.getSiteId() != null && !req.getSiteId().equals(s.getSite().getId())) {
            Site site = siteRepository.findById(req.getSiteId())
                    .filter(t -> t.getCompany().getId().equals(companyId))
                    .orElseThrow(() -> new EntityNotFoundException("Site not found"));
            s.setSite(site);
        }

        // period type (par défaut = MONTH si non fourni)
        Schedule.PeriodType type = (req.getPeriodType() == null)
                ? Schedule.PeriodType.MONTH
                : req.getPeriodType();

        if (type == Schedule.PeriodType.MONTH) {
            if (req.getMonth() == null || req.getYear() == null)
                throw new IllegalArgumentException("month & year requis pour un planning mensuel");

            s.setPeriodType(Schedule.PeriodType.MONTH);
            s.setMonth(req.getMonth());
            s.setYear(req.getYear());
            s.setStartDate(null);
            s.setEndDate(null);

        } else { // RANGE
            if (req.getStartDate() == null || req.getEndDate() == null)
                throw new IllegalArgumentException("startDate & endDate requis pour un planning RANGE");
            if (req.getEndDate().isBefore(req.getStartDate()))
                throw new IllegalArgumentException("endDate doit être >= startDate");

            s.setPeriodType(Schedule.PeriodType.RANGE);
            s.setStartDate(req.getStartDate());
            s.setEndDate(req.getEndDate());
            s.setMonth(null);
            s.setYear(null);
        }

        // nom « safe » (si non fourni)
        s.setName(produceName(req, s.getSite()));

        return scheduleRepository.save(s);
    }
    @Transactional
    public void deleteSchedule(User actor, Long companyId, Long id) throws AccessDeniedException {
        Schedule s = getSchedule(companyId, id);
        assertCanManage(actor, s.getSite().getId());

        if (s.isPublished() || s.isSent()) {
            throw new IllegalStateException("Impossible de supprimer un planning publié ou déjà envoyé");
        }

        // Puis le planning
        scheduleRepository.delete(s);
    }


    /* ===============================================================
       Lecture / DTO
       =============================================================== */

    public Schedule getSchedule(Long companyId, Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
    }

    private boolean canEdit(User actor, Schedule s) {
        User u = userRepository.findByIdWithSites(actor.getId())
                .orElseThrow();
        return u.isSuperAdmin()
                || u.isCompanyAdminGlobal()
                || u.canAccessSite(s.getSite().getId());
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByFilters(User actor,
                                                        Long companyId,
                                                        Long siteId,
                                                        Integer month,
                                                        Integer year,
                                                        Boolean published) {
        // Vérif acteur
        User u = userRepository.findByIdWithSites(actor.getId()).orElseThrow();
        if (!u.isSuperAdmin() && !u.isCompanyAdminGlobal()) {
            if (u.getCompany() == null || !u.getCompany().getId().equals(companyId)) {
                throw new org.springframework.security.access.AccessDeniedException("Accès refusé à cette entreprise");
            }
        }

        // Lecture
        List<Schedule> list = scheduleRepository.findByFilters(companyId, siteId, month, year, published);

        // Mapping léger
        return list.stream()
                .map(s -> toDtoLight(actor, s))
                .toList();
    }

    public List<Schedule> getSchedulesBySite(Long companyId, Long siteId) {
        siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found or not owned by company"));
        return scheduleRepository.findBySiteId(siteId);
    }

    /* ===============================================================
       Assignations
       =============================================================== */

    @Transactional
    public ScheduleAssignment addAssignment(User actor, Long companyId,
                                            Long scheduleId,
                                            ScheduleAssignmentRequest req) throws AccessDeniedException  {
        assertCanManage(actor, req.getSiteId());

        // 1) Planning
        Schedule schedule = getSchedule(companyId, scheduleId);
        if (!schedule.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Le planning ne correspond pas à cette entreprise.");
        }

        // 2) Employé appartenant au site du planning
        Employee employee = schedule.getSite().getEmployees().stream()
                .filter(e -> e.getId().equals(req.getEmployeeId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Employé non trouvé dans ce site."));

        // 3) Site sécurisé
        Site site = siteRepository.findById(req.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site non trouvé."));
        if (!site.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Le site ne fait pas partie de l'entreprise.");
        }

        // 4) Date/horaires
        LocalDate   date      = req.getDate();
        LocalTime   startTime = req.getStartTime();
        LocalTime   endTime   = req.getEndTime();

        // 5) Absence
        if (absenceRepository.existsByEmployeeIdAndDate(employee.getId(), date)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L’employé est en absence le " + date);
        }

        // 6) Préférences week-end/nuit
        EmployeePreference p = employee.getPreference();
        DayOfWeek dow = date.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        if (p != null) {
            if (isWeekend && !p.isCanWorkWeekends()) {
                throw new IllegalArgumentException("L’employé ne peut pas travailler le week-end.");
            }
            boolean startsAtNight = startTime.isAfter(LocalTime.of(20, 59));
            boolean endsAtNight   = endTime.isBefore(LocalTime.of(5, 1));
            if ((startsAtNight || endsAtNight) && !p.isCanWorkNights()) {
                throw new IllegalArgumentException("L’employé ne peut pas travailler de nuit.");
            }
        }

        // 7) Chevauchements + repos ≥ 12h (sur toutes les assignations de l'employé)
        List<ScheduleAssignment> existing = assignmentRepository.findByEmployeeId(employee.getId());

        LocalDateTime[] n = buildInterval(date, startTime, endTime);
        LocalDateTime newStart = n[0];
        LocalDateTime newEnd   = n[1];

        for (ScheduleAssignment other : existing) {
            LocalDateTime[] o = buildInterval(other.getDate(), other.getStartTime(), other.getEndTime());
            LocalDateTime oStart = o[0];
            LocalDateTime oEnd   = o[1];

            // chevauchement
            if (newStart.isBefore(oEnd) && newEnd.isAfter(oStart)) {
                throw new IllegalArgumentException("Chevauchement avec une autre vacation.");
            }

            // repos >= 12h
            long gap1 = Duration.between(oEnd, newStart).toHours(); // pause après
            long gap2 = Duration.between(newEnd, oStart).toHours(); // pause avant
            if ((gap1 >= 0 && gap1 < 12) || (gap2 >= 0 && gap2 < 12)) {
                throw new IllegalArgumentException("Il faut au moins 12 h de repos entre deux vacations.");
            }
        }

        // 8) Durée minutes (gère le passage de minuit)
        long durationMin = Duration.between(startTime, endTime).toMinutes();
        if (durationMin <= 0) durationMin += 24 * 60;

        // 9) Persistance
        ScheduleAssignment assignment = ScheduleAssignment.builder()
                .schedule(schedule)
                .employee(employee)
                .site(site)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .duration((int) durationMin)
                .agentType(req.getAgentType())
                .notes(req.getNotes())
                .shift(req.getShift())
                .status(ScheduleAssignment.AssignmentStatus.PENDING)
                .build();

        return assignmentRepository.save(assignment);
    }

    /** Construit l'intervalle temporel absolu d'une vacation (gère le passage de minuit). */
    public static LocalDateTime[] buildInterval(LocalDate date, LocalTime start, LocalTime end) {
        LocalDateTime startDT = LocalDateTime.of(date, start);
        LocalDateTime endDT   = LocalDateTime.of(date, end);
        if (!end.isAfter(start)) { // passe minuit
            endDT = endDT.plusDays(1);
        }
        return new LocalDateTime[]{ startDT, endDT };
    }

    @Transactional
    public ScheduleAssignment updateAssignment(User actor, Long assignmentId, ScheduleAssignmentRequest request) throws AccessDeniedException {
        ScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation non trouvée"));

        // droits
        assertCanManage(actor, assignment.getSchedule().getSite().getId()); // ✅ site du planning toujours présent
        if (request.getSiteId() != null) {
            assertCanManage(actor, request.getSiteId());
        }

        assignment.setDate(request.getDate());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());

        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site non trouvé"));
        assignment.setSite(site);

        assignment.setNotes(request.getNotes());
        assignment.setAgentType(request.getAgentType());
        assignment.setShift(request.getShift());

        // Recalcul durée
        long durationMin = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (durationMin <= 0) durationMin += 24 * 60;
        assignment.setDuration((int) durationMin);

        return assignmentRepository.save(assignment);
    }


    @Transactional
    public void deleteAssignment(User actor, Long companyId, Long scheduleId, Long assignmentId)
            throws AccessDeniedException {

        ScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation non trouvée : " + assignmentId));

        if (!assignment.getSchedule().getId().equals(scheduleId)
                || !assignment.getSchedule().getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n’êtes pas autorisé·e à supprimer cette affectation");
        }

        // ✅ sécurisation : si le site sur l’affectation est nul, on prend le site du planning
        Long siteId = (assignment.getSite() != null)
                ? assignment.getSite().getId()
                : assignment.getSchedule().getSite().getId();

        assertCanManage(actor, siteId);

        assignmentRepository.delete(assignment);
    }


    @Transactional
    public Schedule validateSchedule(User actor, Long companyId, Long id) throws AccessDeniedException {
        Schedule s = getSchedule(companyId, id);
        assertCanManage(actor, s.getSite().getId());
        s.setValidated(true);
        return scheduleRepository.save(s);
    }

    private String deriveShiftLabel(LocalTime start, LocalTime end) {
        if (start.isBefore(LocalTime.NOON)) return "MATIN";
        if (start.isAfter(LocalTime.of(14, 0))) return "SOIR";
        return "JOUR";
    }

    /* ===============================================================
       Publication / Envoi
       =============================================================== */

    @Transactional
    public Schedule sendSchedule(User actor, Long companyId, Long scheduleId) throws AccessDeniedException {
        Schedule s = getSchedule(companyId, scheduleId);
        assertCanManage(actor, s.getSite().getId());

        Map<Long, List<ScheduleAssignment>> byEmp = assignmentRepository
                .findByScheduleId(scheduleId)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        String periodLabel;
        if (s.getPeriodType() == Schedule.PeriodType.MONTH && s.getMonth() != null && s.getYear() != null) {
            periodLabel = String.format("%02d/%04d", s.getMonth(), s.getYear());
        } else if (s.getStartDate() != null && s.getEndDate() != null) {
            periodLabel = String.format("%1$td/%1$tm/%1$tY → %2$td/%2$tm/%2$tY", s.getStartDate(), s.getEndDate());
        } else {
            periodLabel = "";
        }

        byEmp.forEach((empId, assigns) -> {
            String email = assigns.get(0).getEmployee().getEmail();
            byte[] pdf = planningPdfService.generatePdfForEmployee(s, assigns);
            String subj = "Votre planning " + s.getSite().getName() +
                    (periodLabel.isBlank() ? "" : " " + periodLabel);
            String body = "Bonjour " + assigns.get(0).getEmployee().getFirstName()
                    + ",\n\nVeuillez trouver ci-joint votre planning.";
            mailService.sendSchedulePdfToEmployee(email, subj, body, pdf);
        });

        s.setSent(true);
        s.setSentAt(LocalDateTime.now());
        return scheduleRepository.save(s);
    }

    @Transactional(readOnly = true)
    public Schedule getForSending(Long scheduleId) {
        return scheduleRepository.findByIdWithSiteAndCompany(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
    }

    @Transactional
    public Schedule publishSchedule(User actor, Long companyId, Long id) throws AccessDeniedException {
        Schedule s = getSchedule(companyId, id);
        assertCanManage(actor, s.getSite().getId());
        s.setPublished(true);
        scheduleRepository.save(s);

        notificationService.notifyCompany(
                s.getCompany(),
                "Le planning \"" + s.getName() + "\" a été publié",
                "calendar-check"
        );

        return s;
    }

    /* ===============================================================
       Mapping DTO
       =============================================================== */

    // Builder commun
    private ScheduleResponse.ScheduleResponseBuilder baseDto(Schedule s) {
        return ScheduleResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .month(s.getMonth())
                .year(s.getYear())
                .published(s.isPublished())
                .validated(s.isValidated())
                .sent(s.isSent())
                .sentAt(s.getSentAt())
                .completionRate(s.getCompletionRate())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .site(ScheduleResponse.SiteInfo.builder()
                        .id(s.getSite().getId())
                        .name(s.getSite().getName())
                        .city(s.getSite().getCity())
                        .address(s.getSite().getAddress())
                        .email(s.getSite().getEmail())
                        .phone(s.getSite().getPhone())
                        .build())
                .company(ScheduleResponse.CompanyInfo.builder()
                        .id(s.getCompany().getId())
                        .name(s.getCompany().getName())
                        .email(s.getCompany().getEmail())
                        .phone(s.getCompany().getPhone())
                        .website(s.getCompany().getWebsite())
                        .build());
    }

    private ScheduleResponse.Permissions computePerms(User actor, Schedule s) {
        boolean edit      = canEdit(actor, s);
        boolean generate  = edit && !s.isPublished();
        boolean publish   = edit && !s.isPublished();
        boolean send      = edit && s.isPublished() && !s.isSent();
        return ScheduleResponse.Permissions.builder()
                .edit(edit)
                .generate(generate)
                .publish(publish)
                .send(send)
                .build();
    }

    // Version liste (légère)
    public ScheduleResponse toDtoLight(User actor, Schedule s) {
        return baseDto(s)
                .canEdit(canEdit(actor, s))
                .permissions(computePerms(actor, s))
                .build();
    }

    // Version détail (avec affectations)
    public ScheduleResponse toDto(User actor, Schedule s, List<ScheduleAssignment> assignments) {
        return baseDto(s)
                .assignments(assignments.stream().map(AssignmentDTO::of).toList())
                .canEdit(canEdit(actor, s))
                .permissions(computePerms(actor, s))
                .build();
    }

    /* ===============================================================
       Accès internes
       =============================================================== */

    /** Renvoie le Schedule sans contrôle d’entreprise (usage interne). */
    public Schedule getEntity(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found, id=" + scheduleId));
    }

    /** Toutes les affectations d’un planning. */
    public List<ScheduleAssignment> getAssignments(Long scheduleId) {
        return assignmentRepository.findByScheduleId(scheduleId);
    }

    /** Vérifie qu’un employé appartient bien au planning et le renvoie. */
    public Employee getEmployeeInSchedule(Long scheduleId, Long empId) {
        return getAssignments(scheduleId).stream()
                .map(ScheduleAssignment::getEmployee)
                .filter(e -> e.getId().equals(empId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee " + empId + " not found in schedule " + scheduleId));
    }

    public EmployeePlanningDTO getEmployeePlanning(Long scheduleId, Long empId) {
        Schedule sched = getEntity(scheduleId);

        List<ScheduleAssignment> assigns =
                assignmentRepository.findByScheduleIdAndEmployeeId(scheduleId, empId);

        Map<LocalDate, List<AssignmentDTO>> calendar = assigns.stream()
                .map(AssignmentDTO::of)
                .collect(Collectors.groupingBy(AssignmentDTO::getDate));

        Employee emp = assigns.isEmpty()
                ? getEmployeeInSchedule(scheduleId, empId)
                : assigns.get(0).getEmployee();

        return EmployeePlanningDTO.builder()
                .scheduleId(scheduleId)
                .month(sched.getMonth())
                .year(sched.getYear())
                .employeeId(empId)
                .employeeName(emp.getFirstName() + " " + emp.getLastName())
                .calendar(calendar)
                .build();
    }
}
