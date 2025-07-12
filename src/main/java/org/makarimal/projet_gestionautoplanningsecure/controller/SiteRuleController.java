package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteScheduleOverrideRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteShiftRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.WeeklyScheduleRuleRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteScheduleOverride;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteShift;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.model.WeeklyScheduleRule;
import org.makarimal.projet_gestionautoplanningsecure.service.SiteRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sites/{siteId}")
@RequiredArgsConstructor
public class SiteRuleController {

    private final SiteRuleService siteRuleService;

    /* ----------   VACATIONS   ---------- */

    @PostMapping("/shifts")
    public ResponseEntity<SiteShift> createShift(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody SiteShiftRequest request) {

        SiteShift shift = siteRuleService.createShift(
                user.getCompany().getId(), siteId, request);
        return ResponseEntity.ok(shift);
    }

    @GetMapping("/shifts")
    public ResponseEntity<List<SiteShift>> getSiteShifts(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId) {

        return ResponseEntity.ok(
                siteRuleService.getSiteShifts(user.getCompany().getId(), siteId));
    }

    /* ----------   RÈGLES HEBDOMADAIRES   ---------- */

    @PostMapping("/weekly-schedule-rule")
    public ResponseEntity<List<WeeklyScheduleRule>> defineWeeklyRule(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody List<WeeklyScheduleRuleRequest> requests) {

        Long companyId = user.getCompany().getId();
        List<WeeklyScheduleRule> rules =
                siteRuleService.defineWeeklyScheduleRule(companyId, siteId, requests);
        return ResponseEntity.ok(rules);
    }

    /* ----------   OVERRIDES   ---------- */

    @PostMapping("/schedule-override")
    public ResponseEntity<SiteScheduleOverride> createOverride(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody SiteScheduleOverrideRequest request) {

        SiteScheduleOverride override =
                siteRuleService.createOrUpdateOverride(user.getCompany().getId(), siteId, request);
        return ResponseEntity.ok(override);
    }

    @GetMapping("/weekly-schedule-rule")
    public ResponseEntity<List<WeeklyScheduleRule>> getWeeklyRule(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId) {

        List<WeeklyScheduleRule> rules = siteRuleService.getWeeklyScheduleRules(
                user.getCompany().getId(), siteId);

        return ResponseEntity.ok(rules);
    }


    @PutMapping("/weekly-schedule-rule")
    public ResponseEntity<List<WeeklyScheduleRule>> updateWeeklyRules(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody List<WeeklyScheduleRuleRequest> requests) {

        List<WeeklyScheduleRule> updated = siteRuleService.replaceWeeklyRules(
                user.getCompany().getId(), siteId, requests);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/weekly-schedule-rule")
    public ResponseEntity<Void> deleteAllWeeklyRules(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId) {

        siteRuleService.deleteWeeklyRules(user.getCompany().getId(), siteId);
        return ResponseEntity.noContent().build();
    }




}
