package org.makarimal.projet_gestionautoplanningsecure.controller;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SimpleSiteDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {
    private final UserRepository userRepository;

    @GetMapping("/sites-managed")
    public List<SimpleSiteDTO> getManagedSites(@AuthenticationPrincipal User actor) {
        User u = userRepository.findByIdWithSites(actor.getId()).orElseThrow();
        if (u.isSuperAdmin() || u.isCompanyAdminGlobal() || u.isManageAllSites()) {
            return u.getCompany().getSites().stream()
                    .map(s -> new SimpleSiteDTO(s.getId(), s.getName()))
                    .toList();
        }
        return u.getManagedSites().stream()
                .map(s -> new SimpleSiteDTO(s.getId(), s.getName()))
                .toList();
    }
}

