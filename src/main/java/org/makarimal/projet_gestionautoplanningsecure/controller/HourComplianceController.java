package org.makarimal.projet_gestionautoplanningsecure.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.ContractHourRequirement;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.service.HourComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hour-compliance")
@RequiredArgsConstructor
public class HourComplianceController {

    private final HourComplianceService complianceService;

    @PostMapping("/requirements/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContractHourRequirement>> initializeRequirements(
            @AuthenticationPrincipal User user) {
        List<ContractHourRequirement> requirements = complianceService
                .initializeDefaultRequirements(user.getCompany().getId());
        return ResponseEntity.ok(requirements);
    }

    @GetMapping("/requirements")
    public ResponseEntity<List<ContractHourRequirement>> getRequirements(
            @AuthenticationPrincipal User user) {
        List<ContractHourRequirement> requirements = complianceService
                .getRequirements(user.getCompany().getId());
        return ResponseEntity.ok(requirements);
    }

    @PutMapping("/requirements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContractHourRequirement> updateRequirement(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ContractHourRequirementRequest request) {
        ContractHourRequirement updated = complianceService
                .updateRequirement(user.getCompany().getId(), request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleComplianceResponseDTO> getScheduleCompliance(
            @AuthenticationPrincipal User user,
            @PathVariable Long scheduleId) {
        ScheduleComplianceResponseDTO compliance = complianceService
                .getScheduleCompliance(user.getCompany().getId(), scheduleId);
        return ResponseEntity.ok(compliance);
    }

    @GetMapping("/monthly")
    public ResponseEntity<ScheduleComplianceResponseDTO> getMonthlyCompliance(
            @AuthenticationPrincipal User user,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        ScheduleComplianceResponseDTO compliance = complianceService
                .getMonthlyCompliance(user.getCompany().getId(), month, year);
        return ResponseEntity.ok(compliance);
    }
}