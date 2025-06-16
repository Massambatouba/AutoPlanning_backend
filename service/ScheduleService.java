package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.AssignmentDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.ScheduleAssignmentRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.ScheduleRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.ScheduleResponse;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private final AbsenceRepository absenceRepository;

    /* ===============================================================
       CREATE  ▸ ou ▸  REFRESH  (si déjà un schedule même clé unique)
       =============================================================== */
/* ===============================================================
   CREATE  ▸ ou ▸  REFRESH
   =============================================================== */
    @Transactional
    public Schedule createOrRefresh(Long companyId, ScheduleRequest req) {

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
                    assignmentRepository.findAllForCompany(companyId);
            assignmentRepository.deleteAll(old);

            // remise à zéro des indicateurs
            schedule.setCompletionRate(0);
            schedule.setPublished(false);
            schedule.setSent(false);
            schedule.setSentAt(null);

            /* >>>>  on remet AUSSI le nom  <<<< */
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
    public Schedule updateSchedule(Long companyId, Long scheduleId, ScheduleRequest request) {
        Schedule schedule = getSchedule(companyId, scheduleId);

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



    public List<Schedule> getSchedulesByFilters(Long companyId,
                                                Long siteId,
                                                Integer month,
                                                Integer year,
                                                Boolean published) {
        // -> à implémenter (ex. via Specification / QueryDsl)
        return scheduleRepository.findByFilters(
                companyId,
                siteId,
                month,
                year,
                published
        );
    }

    public List<Schedule> getSchedulesBySite(Long companyId, Long siteId) {
        if (!siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Site not found or not owned by company");
        }
        return scheduleRepository.findBySiteId(siteId);
    }

    public Schedule publishSchedule(Long companyId, Long id) {
        Schedule s = getSchedule(companyId, id);
        s.setPublished(true);
        return scheduleRepository.save(s);
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
    public ScheduleAssignment addAssignment(Long companyId,
                                            Long scheduleId,
                                            ScheduleAssignmentRequest req) {
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
        List<ScheduleAssignment> existing = assignmentRepository
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

    @Transactional
    public ScheduleAssignment updateAssignment(Long assignmentId, ScheduleAssignmentRequest request) {
        ScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation non trouvée"));

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
    public void deleteAssignment(Long companyId, Long scheduleId, Long assignmentId) {
        // 1) Charger l’affectation
        ScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Affectation non trouvée : " + assignmentId));

        // 2) Vérifier que l’affectation appartient bien au planning et à l’entreprise
        if (!assignment.getSchedule().getId().equals(scheduleId)
                || !assignment.getSchedule().getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n’êtes pas autorisé·e à supprimer cette affectation");
        }

        // 3) Supprimer
        assignmentRepository.delete(assignment);
    }

    public void handleUnjustifiedAbsence(Long employeeId, LocalDate date) {
    }

    @Transactional
    public Schedule validateSchedule(Long companyId, Long id) {
        Schedule s = getSchedule(companyId, id);
        s.setValidated(true);
        return scheduleRepository.save(s);
    }

    private String deriveShiftLabel(LocalTime start, LocalTime end) {
        if (start.isBefore(LocalTime.NOON)) return "MATIN";
        if (start.isAfter(LocalTime.of(14, 0))) return "SOIR";
        return "JOUR";
    }


    @Transactional
    public Schedule sendSchedule(Long companyId, Long scheduleId) {
        Schedule s = getSchedule(companyId, scheduleId);

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

    public ScheduleResponse toDto(Schedule schedule, List<ScheduleAssignment> assignments) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .month(schedule.getMonth())
                .year(schedule.getYear())
                .published(schedule.isPublished())
                .validated(schedule.isValidated())
                .sent(schedule.isSent())
                .sentAt(schedule.getSentAt())
                .completionRate(schedule.getCompletionRate())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .site(ScheduleResponse.SiteInfo.builder()
                        .id(schedule.getSite().getId())
                        .name(schedule.getSite().getName())
                        .city(schedule.getSite().getCity())
                        .address(schedule.getSite().getAddress())
                        .email(schedule.getSite().getEmail())
                        .phone(schedule.getSite().getPhone())
                        .build())
                .company(ScheduleResponse.CompanyInfo.builder()
                        .id(schedule.getCompany().getId())
                        .name(schedule.getCompany().getName())
                        .email(schedule.getCompany().getEmail())
                        .phone(schedule.getCompany().getPhone())
                        .website(schedule.getCompany().getWebsite())
                        .build())
                .assignments(assignments.stream().map(AssignmentDTO::of).toList())
                .build();
    }




}
