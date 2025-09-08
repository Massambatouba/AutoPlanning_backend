package org.makarimal.projet_gestionautoplanningsecure.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeLiteDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.makarimal.projet_gestionautoplanningsecure.service.SiteEmployeeService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/sites/{siteId}/employees")
public class SiteEmployeeController {
    private final SiteEmployeeService service;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/candidates")
    public List<EmployeeLiteDTO> candidates(@PathVariable Long siteId,
                                            @RequestParam(required = false) String q) {
        return service.searchCandidates(siteId, q);
    }

    @GetMapping
    public List<EmployeeLiteDTO> list(@PathVariable Long siteId) {
        return employeeRepository.findBySiteIdOrderByLastNameAsc(siteId)
                .stream().map(EmployeeLiteDTO::of).toList();
    }

    @PostMapping("/{employeeId}")
    public EmployeeLiteDTO attach(@PathVariable Long siteId, @PathVariable Long employeeId) {
        Employee e = service.attach(siteId, employeeId);
        log.info("[API] Attach -> emp={}, site={}", e.getId(), siteId);
        return EmployeeLiteDTO.of(e);
    }
}

