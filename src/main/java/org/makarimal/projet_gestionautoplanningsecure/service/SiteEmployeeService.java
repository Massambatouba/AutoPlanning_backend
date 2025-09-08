package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeLiteDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// SiteEmployeeService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class SiteEmployeeService {
    private final SiteRepository siteRepo;
    private final EmployeeRepository empRepo;

    @Transactional(readOnly = true)
    public List<EmployeeLiteDTO> searchCandidates(Long siteId, String q) {
        Site site = siteRepo.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable"));

        return empRepo.searchCandidatesForSite(site.getCompany().getId(), siteId,
                        q == null ? "" : q.trim().toLowerCase())
                .stream().map(EmployeeLiteDTO::of).toList();
    }


    @Transactional
    public Employee attach(Long siteId, Long employeeId) {
        Site site = siteRepo.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + siteId));
        Employee e = empRepo.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + employeeId));

        e.setSite(site);
        // force l'écriture immédiate dans la même requête HTTP
        e = empRepo.saveAndFlush(e);

        // trace utile au debug
        log.info("[ATTACH] emp={} -> site={} (en base maintenant)", e.getId(),
                e.getSite() != null ? e.getSite().getId() : null);
        return e;
    }
}

