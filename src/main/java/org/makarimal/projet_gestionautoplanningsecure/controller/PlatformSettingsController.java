package org.makarimal.projet_gestionautoplanningsecure.controller;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.PlatformSettingsView;
import org.makarimal.projet_gestionautoplanningsecure.dto.UpdatePlatformSettingsRequest;
import org.makarimal.projet_gestionautoplanningsecure.service.PlatformSettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform-admin/settings")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformSettingsController {

    private final PlatformSettingsService service;

    @GetMapping
    public PlatformSettingsView get(){ return service.getView(); }

    @PutMapping
    public PlatformSettingsView update(@RequestBody UpdatePlatformSettingsRequest req){
        return service.update(req);
    }
}

