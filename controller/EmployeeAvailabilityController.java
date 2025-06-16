package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeAvailabilityRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeePreferenceRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAvailability;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeePreference;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.service.EmployeeAvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees/{employeeId}")
@RequiredArgsConstructor
public class EmployeeAvailabilityController {
    private final EmployeeAvailabilityService availabilityService;

    @PostMapping("/availabilities")
    public ResponseEntity<EmployeeAvailability> addAvailability(
            @AuthenticationPrincipal User user,
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeeAvailabilityRequest request
    ) {
        EmployeeAvailability availability = availabilityService.addAvailability(
                user.getCompany().getId(),
                employeeId,
                request
        );
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/availabilities")
    public ResponseEntity<List<EmployeeAvailability>> getAvailabilities(
            @AuthenticationPrincipal User user,
            @PathVariable Long employeeId
    ) {
        List<EmployeeAvailability> availabilities = availabilityService.getAvailabilities(
                user.getCompany().getId(),
                employeeId
        );
        return ResponseEntity.ok(availabilities);
    }

    @DeleteMapping("/availabilities/{availabilityId}")
    public ResponseEntity<Void> deleteAvailability(
            @AuthenticationPrincipal User user,
            @PathVariable Long employeeId,
            @PathVariable Long availabilityId
    ) {
        availabilityService.deleteAvailability(
                user.getCompany().getId(),
                employeeId,
                availabilityId
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/preferences")
    public ResponseEntity<EmployeePreference> updatePreferences(
            @AuthenticationPrincipal User user,
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeePreferenceRequest request
    ) {
        EmployeePreference preferences = availabilityService.updatePreferences(
                user.getCompany().getId(),
                employeeId,
                request
        );
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/preferences")
    public ResponseEntity<EmployeePreference> getPreferences(
            @AuthenticationPrincipal User user,
            @PathVariable Long employeeId
    ) {
        EmployeePreference preferences = availabilityService.getPreferences(
                user.getCompany().getId(),
                employeeId
        );
        return ResponseEntity.ok(preferences);
    }
}