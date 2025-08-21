package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.mail.MessagingException;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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



    private void assertCanManage(User actor, Long siteId) throws AccessDeniedException {
        User u = userRepository.findByIdWithSites(actor.getId())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));
        if (!u.canAccessSite(siteId)) {
            throw new AccessDeniedException("Vous n’avez pas le droit de gérer ce site");
        }
    }
/* ===============================================================
   CREATE  ▸ ou ▸  REFRESH
   =============================================================== */
    @Transactional
    public Schedule createOrRefresh(User actor, Long companyId, ScheduleRequest req) throws AccessDeniedException {

        assertCanManage(actor, req.getSiteId());
        /* -------- Sécurité -------- */
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));

        Site site = siteRepository.findById(req.getSiteId())
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() ->
                        new EntityNotFoundException("Site not found or not owned by company"));

        /* -------- Recherche par clé unique -------- */
        Schedule schedule = scheduleRepository
                .findBySiteIdAndMonthAndYear(site.getId(), req.getMonth(), req.getYear())
                .orElse(null);

        /* ========= 1) planning déjà présent → on « rafraîchit » ========= */
        if (schedule != null) {

            // suppression des vieilles affectations
            List<ScheduleAssignment> old =
                    assignmentRepository.findAllForCompany(schedule.getId());
            assignmentRepository.deleteAll(old);

            // remise à zéro des indicateurs
            schedule.setCompletionRate(0);
            schedule.setPublished(false);
            schedule.setSent(false);
            schedule.setSentAt(null);
            schedule.setName( produceName(req, site) );

        }
        /* ========= 2) sinon → on crée un nouveau planning =============== */
        else {
            schedule = Schedule.builder()
                    .company(company)
                    .site(site)
                    .name( produceName(req, site) )        // ← jamais null
                    .month(req.getMonth())
                    .year(req.getYear())
                    .published(false)
                    .sent(false)
                    .completionRate(0)
                    .build();
        }

        /* -------- Persistance -------- */
        schedule = scheduleRepository.save(schedule);

        log.info("Schedule [{}] ({}/{}) sauvegardé, id={}",
                schedule.getName(), schedule.getMonth(),
                schedule.getYear(), schedule.getId());

        return schedule;
    }

    @Transactional
    public Schedule updateSchedule(User actor, Long companyId, Long scheduleId, ScheduleRequest request) throws AccessDeniedException {

        Schedule schedule = getSchedule(companyId, scheduleId);

        assertCanManage(actor, schedule.getSite().getId());
        assertCanManage(actor, request.getSiteId());

        if (schedule.isPublished()) {
            throw new IllegalStateException("Cannot update published schedule");
        }

        Site site = siteRepository.findById(request.getSiteId())
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        schedule.setSite(site);
        schedule.setName(request.getName());
        schedule.setMonth(request.getMonth());
        schedule.setYear(request.getYear());

        return scheduleRepository.save(schedule);
    }

/* ------------------------------------------------------------------
   Petites aides
   ------------------------------------------------------------------ */

    /** Construit un nom « safe » : si le client ne l’a pas fourni,
     *  on utilise :  « <Nom du site> - MM/YYYY ».
     */
    private String produceName(ScheduleRequest req, Site site) {
        if (req.getName() != null && !req.getName().isBlank()) {
            return req.getName();
        }
        return String.format("%s - %02d/%d",          // ex : Siège social - 06/2025
                site.getName(), req.getMonth(), req.getYear());
    }


    /* ===============================================================
       Autres méthodes simplifiées — juste pour compiler l’exemple
       =============================================================== */

    public Schedule getSchedule(Long companyId, Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
    }

    private List<Long> allowedSiteIds(User u, Long companyId) {
        // SUPER/ADMIN global : tous les sites de l’entreprise
        if (u.isSuperAdmin() || u.isCompanyAdminGlobal() || u.isManageAllSites()) {
            return siteRepository.findIdsByCompanyId(companyId);
        }
        // SITE_ADMIN avec liste
        return u.getManagedSites().stream().map(Site::getId).toList();
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
        // 1) Recharger l’acteur (avec ses sites) et vérifier la société
        User u = userRepository.findByIdWithSites(actor.getId()).orElseThrow();
        if (!u.isSuperAdmin() && !u.isCompanyAdminGlobal()) {
            // Un SITE_ADMIN ne doit voir que sa société
            if (u.getCompany() == null || !u.getCompany().getId().equals(companyId)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Accès refusé à cette entreprise");
            }
        }

        // 2) Lecture : on ne restreint pas par sites (lecture globale dans la société)
        List<Schedule> list = scheduleRepository.findByFilters(
                companyId, siteId, month, year, published
        );

        // 3) Mapper en DTO + calculer canEdit pour chaque planning
        return list.stream()
                .map(s -> toDtoLight(actor, s)) // DTO léger, voir ci‑dessous
                .toList();
    }


    public List<Schedule> getSchedulesBySite(Long companyId, Long siteId) {
        if (!siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Site not found or not owned by company");
        }
        return scheduleRepository.findBySiteId(siteId);
    }

    /*public Schedule sendSchedule(Long companyId, Long id) {
        Schedule s = getSchedule(companyId, id);
        s.setSent(true);
        s.setSentAt(java.time.LocalDateTime.now());
        return scheduleRepository.save(s);
    }

     */
/*
    public ScheduleAssignment addAssignment(Long companyId, Long scheduleId,
                                            org.makarimal.projet_gestionautoplanningsecure.dto
                                                    .ScheduleAssignmentRequest req) {
        // TODO : créer et enregistrer l’affectation manuelle
        throw new UnsupportedOperationException("Not implemented yet");
    }

 */

    @Transactional
    public ScheduleAssignment addAssignment(User actor, Long companyId,
                                            Long scheduleId,
                                            ScheduleAssignmentRequest req) throws AccessDeniedException  {
        assertCanManage(actor, req.getSiteId());
        // 1) Récupérer le planning et vérifier l’appartenance à l’entreprise
        Schedule schedule = getSchedule(companyId, scheduleId);
        if (!schedule.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Le planning ne correspond pas à cette entreprise.");
        }

        // 2) Récupérer l’employé affecté au site du planning
        Employee employee = schedule.getSite().getEmployees().stream()
                .filter(e -> e.getId().equals(req.getEmployeeId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Employé non trouvé dans ce site."));

        // 3) Récupérer et sécuriser le site
      /*  Long principalSiteId = schedule.getSite().getId();
        if (!principalSiteId.equals(req.getSiteId()) &&
                (employee.getPreferredSites() == null ||
                        !employee.getPreferredSites().contains(req.getSiteId()))) {
            throw new IllegalArgumentException("Le site n’est pas autorisé pour cet employé.");
        }

       */
        Site site = siteRepository.findById(req.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site non trouvé."));

        if (!site.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Le site ne fait pas partie de l'entreprise.");
        }

        // 4) Préparer les horaires et la date
        LocalDate   date      = req.getDate();
        LocalTime   startTime = req.getStartTime();
        LocalTime   endTime   = req.getEndTime();

        // 5) Vérifier absence
        if (absenceRepository.existsByEmployeeIdAndDate(employee.getId(), date)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L’employé est en absence le " + date
            );
        }

// 6) Vérifier préférences week-end / nuit
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

        // 7) Vérifier chevauchements et 12h de repos mini
     /*   List<ScheduleAssignment> existing = assignmentRepository
                .findByEmployeeIdAndDate(employee.getId(), date);
        LocalDateTime newStart = LocalDateTime.of(date, startTime);
        LocalDateTime newEnd   = LocalDateTime.of(date, endTime);
        for (ScheduleAssignment other : existing) {
            LocalDateTime oStart = LocalDateTime.of(other.getDate(), other.getStartTime());
            LocalDateTime oEnd   = LocalDateTime.of(other.getDate(), other.getEndTime());
            // chevauchement
            if (newStart.isBefore(oEnd) && newEnd.isAfter(oStart)) {
                throw new IllegalArgumentException("Chevauchement avec une autre vacation.");
            }
            // 12h de repos
            long gapBefore = Duration.between(oEnd, newStart).toHours();
            long gapAfter  = Duration.between(newEnd, oStart).toHours();
            if ((gapBefore >= 0 && gapBefore < 12) ||
                    (gapAfter  >= 0 && gapAfter  < 12)) {
                throw new IllegalArgumentException("Au moins 12h de repos entre deux vacations.");
            }
        }

      */
        // 7) Vérifier chevauchements et 12 h de repos
        List<ScheduleAssignment> existing =
                assignmentRepository.findByEmployeeId(employee.getId()); // ← plus seulement le même jour

        LocalDateTime[] n = buildInterval(date, startTime, endTime);
        LocalDateTime newStart = n[0];
        LocalDateTime newEnd   = n[1];

        for (ScheduleAssignment other : existing) {

            LocalDateTime[] o = buildInterval(
                    other.getDate(),
                    other.getStartTime(),
                    other.getEndTime());

            LocalDateTime oStart = o[0];
            LocalDateTime oEnd   = o[1];

            /* ── chevauchement ─────────────────────── */
            if (newStart.isBefore(oEnd) && newEnd.isAfter(oStart)) {
                throw new IllegalArgumentException(
                        "Chevauchement avec une autre vacation.");
            }

            /* ── repos >= 12 h ──────────────────────── */
            long gap1 = Duration.between(oEnd, newStart).toHours(); // pause après
            long gap2 = Duration.between(newEnd, oStart).toHours(); // pause avant
            if ((gap1 >= 0 && gap1 < 12) || (gap2 >= 0 && gap2 < 12)) {
                throw new IllegalArgumentException(
                        "Il faut au moins 12 h de repos entre deux vacations.");
            }
        }


        // 8) Calculer la durée en minutes
        long durationMin = Duration.between(startTime, endTime).toMinutes();
        if (durationMin <= 0) {
            // si on passe minuit
            durationMin += 24 * 60;
        }

        // 9) Créer et sauvegarder l’affectation
        ScheduleAssignment assignment = ScheduleAssignment.builder()
                .schedule(schedule)
                .employee(employee)
                .site(site)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .duration((int) durationMin)                 // <— durée NON NULL
                .agentType(req.getAgentType())               // <— agentType NON NULL
                .notes(req.getNotes())                       // ou "" si nullable
                .shift(req.getShift())                       // libellé du shift
                .status(ScheduleAssignment.AssignmentStatus.PENDING)
                .build();

        return assignmentRepository.save(assignment);
    }


    // GERER LE CHEVAUCHEMENT
    public static LocalDateTime[] buildInterval(LocalDate date,
                                                 LocalTime start,
                                                 LocalTime end) {
        LocalDateTime startDT = LocalDateTime.of(date, start);
        LocalDateTime endDT   = LocalDateTime.of(date, end);
        if (!end.isAfter(start)) {                // passe minuit
            endDT = endDT.plusDays(1);
        }
        return new LocalDateTime[]{ startDT, endDT };
    }



    @Transactional
    public ScheduleAssignment updateAssignment(User actor, Long assignmentId, ScheduleAssignmentRequest request) throws AccessDeniedException {
        ScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation non trouvée"));

        /* ① droit sur l’ancien site … */
        assertCanManage(actor, assignment.getSite().getId());
        /* ② … et sur le nouveau site (si changé) */
        assertCanManage(actor, request.getSiteId());

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
        if (durationMin <= 0) {
            durationMin += 24 * 60;
        }
        assignment.setDuration((int) durationMin);

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void deleteAssignment(User actor, Long companyId, Long scheduleId, Long assignmentId) throws AccessDeniedException {
        // 1) Charger l’affectation
        ScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation non trouvée : " + assignmentId));

        // 2) Vérifier que l’affectation appartient bien au planning et à l’entreprise
        if (!assignment.getSchedule().getId().equals(scheduleId)
                || !assignment.getSchedule().getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n’êtes pas autorisé·e à supprimer cette affectation");
        }

        /* contrôle d’autorisation */
        assertCanManage(actor, assignment.getSite().getId());

        // 3) Supprimer
        assignmentRepository.delete(assignment);
    }

    public void handleUnjustifiedAbsence(Long employeeId, LocalDate date) {
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


    @Transactional
    public Schedule sendSchedule(User actor, Long companyId, Long scheduleId) throws AccessDeniedException {
        Schedule s = getSchedule(companyId, scheduleId);
        assertCanManage(actor, s.getSite().getId());
        // regrouper les assignments par employé
        Map<Long, List<ScheduleAssignment>> byEmp = assignmentRepository
                .findByScheduleId(scheduleId)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()));

        // pour chaque employé, on génère et on envoie
        byEmp.forEach((empId, assigns) -> {
            String email = assigns.get(0).getEmployee().getEmail();
            byte[] pdf = planningPdfService.generatePdfForEmployee(s, assigns);
            String subj = "Votre planning " + s.getSite().getName()
                    + " " + String.format("%02d/%d", s.getMonth(), s.getYear());
            String body = "Bonjour " + assigns.get(0).getEmployee().getFirstName()
                    + ",\n\nVeuillez trouver ci-joint votre planning.";
            mailService.sendSchedulePdfToEmployee(email, subj, body, pdf);
        });

        // marquer comme envoyé
        s.setSent(true);
        s.setSentAt(LocalDateTime.now());
        return scheduleRepository.save(s);
    }

    // 1) Builder commun (privé)
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

    // 2) Version liste (légère)
    public ScheduleResponse toDtoLight(User actor, Schedule s) {
        return baseDto(s)
                .canEdit(canEdit(actor, s))
                .permissions(computePerms(actor, s))
                .build();
    }

    // 3) Version détail (avec affectations)
    public ScheduleResponse toDto(User actor, Schedule s, List<ScheduleAssignment> assignments) {
        return baseDto(s)
                .assignments(assignments.stream().map(AssignmentDTO::of).toList())
                .canEdit(canEdit(actor, s))
                .permissions(computePerms(actor, s))
                .build();
    }



    /** Renvoie le Schedule sans contrôle d’entreprise (usage interne). */
    public Schedule getEntity(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Schedule not found, id=" + scheduleId));
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

        // Récupère toutes les affectations de cet employé
        List<ScheduleAssignment> assigns =
                assignmentRepository.findByScheduleIdAndEmployeeId(scheduleId, empId);

        // Regrouper par date
        Map<LocalDate, List<AssignmentDTO>> calendar = assigns.stream()
                .map(AssignmentDTO::of)                       // ton mapper DTO
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


    @Transactional(readOnly = true)
    public Schedule getForSending(Long scheduleId) {
        return scheduleRepository.findByIdWithSiteAndCompany(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
    }


    @Transactional
    public Schedule publishSchedule(User actor, Long companyId, Long id) throws AccessDeniedException {

        Schedule s = getSchedule(companyId, id);
        assertCanManage(actor, s.getSite().getId());
        assertCanManage(actor, s.getSite().getId());
        s.setPublished(true);
        scheduleRepository.save(s);

        // 2️⃣ crée une notification pour toute la société
        notificationService.notifyCompany(
                s.getCompany(),
                "Le planning \"" + s.getName() + "\" a été publié",
                "calendar-check"
        );

        return s;
    }




}
