/* src/main/java/.../controller/PlanningController.java */
package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.service.PlanningQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningQueryService planning;

    /* ------------------------------------------------------------------ */
    /* 1. Planning d’un employé                                           */
    /* ------------------------------------------------------------------ */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<EmployeePlanningDTO> getEmployeePlanning(
            @PathVariable Long employeeId,
            @RequestParam @Min(1) @Max(12)  int month,
            @RequestParam                 int year,
            @AuthenticationPrincipal      User user) {

        Map<LocalDate, List<AssignmentDTO>> cal =
                planning.getEmployeePlanning(employeeId, month, year);

        Long scheduleId = planning.findScheduleIdForEmployee(employeeId, month, year);


        EmployeePlanningDTO dto = EmployeePlanningDTO.builder()
                .employeeId(employeeId)
                .employeeName(user.getFirstName() + " " + user.getLastName()) // ou via service
                .calendar(cal)
                .scheduleId(scheduleId)
                .build();

        return ResponseEntity.ok(dto);
    }

    /* ------------------------------------------------------------------ */
    /* 2. Planning d’un site                                              */
    /* ------------------------------------------------------------------ */
    @GetMapping("/site/{siteId}")
    public ResponseEntity<sitePlanningDTO> getSitePlanning(
            @PathVariable Long siteId,
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam                int year) {

        return ResponseEntity.ok(
                planning.getSitePlanning(siteId, month, year));
    }
}
