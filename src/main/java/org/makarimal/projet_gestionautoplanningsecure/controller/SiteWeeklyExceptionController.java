package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteWeeklyExceptionDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteWeeklyExceptionRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteWeeklyException;
import org.makarimal.projet_gestionautoplanningsecure.model.WeeklyExceptionType;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteWeeklyExceptionRepository;
import org.makarimal.projet_gestionautoplanningsecure.service.SiteWeeklyExceptionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sites/{siteId}/weekly-exceptions")
public class SiteWeeklyExceptionController {

    private final SiteRepository siteRepo;
    private final SiteWeeklyExceptionRepository excRepo;
    private final SiteWeeklyExceptionService service;



    /* ========
       DETAIL
       ======== */
    @GetMapping("/{id}")
    public SiteWeeklyExceptionDTO get(@PathVariable Long siteId, @PathVariable Long id) {
        ensureSite(siteId);
        SiteWeeklyException e = excRepo.findById(id)
                .filter(x -> x.getSite().getId().equals(siteId))
                .orElseThrow(() -> new EntityNotFoundException("Exception introuvable"));
        return toDto(e);
    }

    /* =========
       CREATION
       ========= */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SiteWeeklyExceptionDTO create(@PathVariable Long siteId,
                                         @RequestBody @Valid SiteWeeklyExceptionRequest req) {
        Site site = ensureSite(siteId);
        validate(req);

        SiteWeeklyException entity = SiteWeeklyException.builder()
                .site(site)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .daysOfWeek(req.getDaysOfWeek())
                .type(req.getType())
                .agentType(req.getAgentType())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .requiredCount(req.getRequiredCount())
                .minExperience(req.getMinExperience())
                .requiredSkills(req.getRequiredSkills())
                .build();

        return toDto(excRepo.save(entity));
    }

    /* =========
       CREATION BULK (optionnel)
       ========= */
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SiteWeeklyExceptionDTO> bulk(@PathVariable Long siteId,
                                             @RequestBody List<@Valid SiteWeeklyExceptionRequest> reqs) {
        Site site = ensureSite(siteId);
        List<SiteWeeklyException> toSave = reqs.stream().peek(this::validate).map(req ->
                SiteWeeklyException.builder()
                        .site(site)
                        .startDate(req.getStartDate())
                        .endDate(req.getEndDate())
                        .daysOfWeek(req.getDaysOfWeek())
                        .type(req.getType())
                        .agentType(req.getAgentType())
                        .startTime(req.getStartTime())
                        .endTime(req.getEndTime())
                        .requiredCount(req.getRequiredCount())
                        .minExperience(req.getMinExperience())
                        .requiredSkills(req.getRequiredSkills())
                        .build()
        ).toList();
        return excRepo.saveAll(toSave).stream().map(this::toDto).toList();
    }

    /* =======
       UPDATE
       ======= */
    @PutMapping("/{id}")
    public SiteWeeklyExceptionDTO update(@PathVariable Long siteId,
                                         @PathVariable Long id,
                                         @RequestBody @Valid SiteWeeklyExceptionRequest req) {
        ensureSite(siteId);
        validate(req);

        SiteWeeklyException e = excRepo.findById(id)
                .filter(x -> x.getSite().getId().equals(siteId))
                .orElseThrow(() -> new EntityNotFoundException("Exception introuvable"));

        e.setStartDate(req.getStartDate());
        e.setEndDate(req.getEndDate());
        e.setDaysOfWeek(req.getDaysOfWeek());
        e.setType(req.getType());
        e.setAgentType(req.getAgentType());
        e.setStartTime(req.getStartTime());
        e.setEndTime(req.getEndTime());
        e.setRequiredCount(req.getRequiredCount());
        e.setMinExperience(req.getMinExperience());
        e.setRequiredSkills(req.getRequiredSkills());

        return toDto(excRepo.save(e));
    }

    /* =======
       DELETE
       ======= */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long siteId, @PathVariable Long id) {
        ensureSite(siteId);
        SiteWeeklyException e = excRepo.findById(id)
                .filter(x -> x.getSite().getId().equals(siteId))
                .orElseThrow(() -> new EntityNotFoundException("Exception introuvable"));
        excRepo.delete(e);
    }

    /* =====================
       Helpers
       ===================== */

    private Site ensureSite(Long siteId) {
        return siteRepo.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable"));
    }

    /** Règles métier minimales pour éviter des exceptions incohérentes. */
    private void validate(SiteWeeklyExceptionRequest r) {
        if (r.getEndDate().isBefore(r.getStartDate())) {
            throw new IllegalArgumentException("endDate doit être ≥ startDate");
        }

        if (r.getType() == WeeklyExceptionType.CLOSE_DAY) {
            // rien d’autre requis
            return;
        }

        if (r.getType() == WeeklyExceptionType.REPLACE_DAY || r.getType() == WeeklyExceptionType.ADD_SHIFT) {
            if (r.getStartTime() == null || r.getEndTime() == null) {
                throw new IllegalArgumentException("startTime et endTime sont requis pour ADD_SHIFT / REPLACE_DAY");
            }
            if (r.getRequiredCount() == null || r.getRequiredCount() <= 0) {
                throw new IllegalArgumentException("requiredCount > 0 requis pour ADD_SHIFT / REPLACE_DAY");
            }
        }

        if (r.getType() == WeeklyExceptionType.MASK_SHIFT) {
            // start/end optionnels (si absents => on masque “toute la journée” pour l’agentType donné
            // agentType optionnel (si absent => masque quel que soit l’agentType)
        }
    }

    private SiteWeeklyExceptionDTO toDto(SiteWeeklyException e) {
        return SiteWeeklyExceptionDTO.builder()
                .id(e.getId())
                .siteId(e.getSite().getId())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .type(e.getType())
                .agentType(e.getAgentType())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .requiredCount(e.getRequiredCount())
                .minExperience(e.getMinExperience())
                .requiredSkills(e.getRequiredSkills())
                .build();
    }

    @GetMapping
    public List<SiteWeeklyException> list(
            @PathVariable Long siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from == null && to == null) {
            // → retourne TOUTES les exceptions du site
            return service.listAll(siteId);
        }
        if (from == null) from = LocalDate.MIN;
        if (to   == null) to   = LocalDate.MAX;
        // → retourne les exceptions qui chevauchent [from;to]
        return service.listOverlapping(siteId, from, to);
    }
}
