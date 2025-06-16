package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.CreateCompanyRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.makarimal.projet_gestionautoplanningsecure.service.CompanyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyAdminService;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Company> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return ResponseEntity.ok(companyAdminService.createCompany(request));
    }


    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(companyAdminService.getAllCompanies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(companyAdminService.getCompany(id));
    }

    @GetMapping("/company")
    public ResponseEntity<Company> getMyCompany(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = principal.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Company company = userRepository.findById(user.getId())
                .map(User::getCompany)
                .orElse(null);

        if (company != null) {
            // Force le chargement des sites (si lazy)
            company.setSites(siteRepository.findAllByCompanyId(company.getId()));
        }

        return ResponseEntity.ok(company);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Company> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody Company request
    ) {
        return ResponseEntity.ok(companyAdminService.updateCompany(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyAdminService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<Company> toggleCompanyStatus(@PathVariable Long id) {
        return ResponseEntity.ok(companyAdminService.toggleCompanyStatus(id));
    }
}
