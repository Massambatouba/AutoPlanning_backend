package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.AdminCmd;
import org.makarimal.projet_gestionautoplanningsecure.dto.AdminCreateCmd;
import org.makarimal.projet_gestionautoplanningsecure.dto.AdminUpdateCmd;
import org.makarimal.projet_gestionautoplanningsecure.dto.UserDto;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService srv;

    /** Créer un admin (le service filtre les droits finement) */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SITE_ADMIN')")
    @PostMapping
    public UserDto create(@AuthenticationPrincipal User me, @RequestBody @Valid AdminCmd cmd)
            throws Exception {
        return UserDto.of(srv.create(me, cmd));
    }

    /** Remplacer la liste des sites (désactive l’accès global) */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SITE_ADMIN')")
    @PutMapping("{id}/sites")
    public UserDto updateSites(@AuthenticationPrincipal User me,
                               @PathVariable Long id,
                               @RequestBody List<Long> siteIds) throws Exception {
        return UserDto.of(srv.updateSites(me, id, siteIds));
    }

    /** Donne l’accès à tous les sites */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}/grant-all")
    public UserDto grantAllSites(@AuthenticationPrincipal User me,
                                 @PathVariable Long id) throws AccessDeniedException {
        return UserDto.of(srv.grantGlobalAccess(me, id));
    }

    /** (Dé)activation explicite */
    public static record StatusRequest(boolean active) {}
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}/status")
    public UserDto setStatus(@AuthenticationPrincipal User me,
                             @PathVariable Long id,
                             @RequestBody StatusRequest req) throws Exception {
        return UserDto.of(srv.setActive(id, req.active(), me));
    }

    /** Mettre manageAllSites + liste dans une seule API */
    public record AccessUpdate(boolean manageAllSites, List<Long> siteIds) {}
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @PutMapping("/{id}/access")
    public ResponseEntity<UserDto> updateAccess(@AuthenticationPrincipal User me,
                                                @PathVariable Long id,
                                                @RequestBody AccessUpdate req) throws AccessDeniedException {
        return ResponseEntity.ok(
                UserDto.of(srv.updateAccess(me, id, req.manageAllSites(), req.siteIds()))
        );
    }

    /** Liste des admins d’une société */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SITE_ADMIN')")
    @GetMapping("/company/{companyId}")
    public List<UserDto> list(@AuthenticationPrincipal User me,
                              @PathVariable Long companyId) throws AccessDeniedException {
        if (!me.isSuperAdmin() && !companyId.equals(me.getCompanyId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        return srv.listAdmins(companyId);
    }

    /** Suppression */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal User me, @PathVariable Long id) throws Exception {
        if (me.getId().equals(id)) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous supprimer vous-même.");
        }
        srv.delete(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public UserDto getOne(@AuthenticationPrincipal User me,
                          @PathVariable Long id) throws AccessDeniedException {
        User u = srv.getById(id); // à implémenter côté service
        // sécurité simple : même société sauf SUPER_ADMIN
        if (!me.isSuperAdmin() && !u.getCompanyId().equals(me.getCompanyId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        return UserDto.of(u);
    }

}




