package org.makarimal.projet_gestionautoplanningsecure.controller;



import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteResponse;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.service.SiteService;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sites")
@RequiredArgsConstructor
public class SiteController {
    private final SiteService siteService;



    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteResponse> createSite(@Valid @RequestBody SiteRequest request) {
        SiteResponse site = siteService.createSite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(site);
    }


    @GetMapping("/{id}")
    public ResponseEntity<SiteResponse> getSite(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        SiteResponse site = siteService.getSite(user.getCompany().getId(), id);
        return ResponseEntity.ok(site);
    }

    @GetMapping
    public ResponseEntity<List<SiteResponse>> getSites(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active
    ) {
        List<SiteResponse> sites = siteService.getSitesByFilters(
                user.getCompany().getId(),
                city,
                active
        );
        return ResponseEntity.ok(sites);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SiteResponse> updateSite(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody SiteRequest request
    ) {
        SiteResponse site = siteService.updateSite(user.getCompany().getId(), id, request);
        return ResponseEntity.ok(site);
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<SiteResponse> toggleSiteStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) throws ChangeSetPersister.NotFoundException {
        SiteResponse site = siteService.toggleSiteStatus(user.getCompany().getId(), id);
        return ResponseEntity.ok(site);
    }
}