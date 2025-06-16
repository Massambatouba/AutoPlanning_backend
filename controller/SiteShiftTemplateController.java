package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteShiftTemplateRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteShiftTemplate;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.service.SiteShiftTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sites/{siteId}/templates")
@RequiredArgsConstructor
public class SiteShiftTemplateController {
    private final SiteShiftTemplateService templateService;

    @PostMapping
    public ResponseEntity<SiteShiftTemplate> createTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @Valid @RequestBody SiteShiftTemplateRequest request
    ) {
        SiteShiftTemplate template = templateService.createTemplate(
                user.getCompany().getId(),
                siteId,
                request
        );
        return ResponseEntity.ok(template);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<SiteShiftTemplate> getTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long templateId
    ) {
        SiteShiftTemplate template = templateService.getTemplate(
                user.getCompany().getId(),
                siteId,
                templateId
        );
        return ResponseEntity.ok(template);
    }

    @GetMapping
    public ResponseEntity<List<SiteShiftTemplate>> getTemplates(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @RequestParam(required = false) Boolean active
    ) {
        List<SiteShiftTemplate> templates = templateService.getTemplates(
                user.getCompany().getId(),
                siteId,
                active
        );
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<SiteShiftTemplate> updateTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long templateId,
            @Valid @RequestBody SiteShiftTemplateRequest request
    ) {
        SiteShiftTemplate template = templateService.updateTemplate(
                user.getCompany().getId(),
                siteId,
                templateId,
                request
        );
        return ResponseEntity.ok(template);
    }

    @PutMapping("/{templateId}/toggle")
    public ResponseEntity<SiteShiftTemplate> toggleTemplateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long templateId
    ) {
        SiteShiftTemplate template = templateService.toggleTemplateStatus(
                user.getCompany().getId(),
                siteId,
                templateId
        );
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @AuthenticationPrincipal User user,
            @PathVariable Long siteId,
            @PathVariable Long templateId
    ) {
        templateService.deleteTemplate(
                user.getCompany().getId(),
                siteId,
                templateId
        );
        return ResponseEntity.noContent().build();
    }
}
