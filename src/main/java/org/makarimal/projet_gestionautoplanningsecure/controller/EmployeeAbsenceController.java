package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeAbsenceRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Absence;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAbsence;
import org.makarimal.projet_gestionautoplanningsecure.service.EmployeeAbsenceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/absences")
@RequiredArgsConstructor
public class EmployeeAbsenceController {

    private final EmployeeAbsenceService absenceService;

    @PostMapping
    public ResponseEntity<EmployeeAbsence> addAbsence(@RequestBody @Valid EmployeeAbsenceRequest request) {
        return ResponseEntity.ok(absenceService.addAbsence(request));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeAbsence>> getAbsences(@PathVariable Long employeeId) {
        return ResponseEntity.ok(absenceService.getEmployeeAbsences(employeeId));
    }


    @PostMapping("/unjustified")
    public ResponseEntity<Void> createUnjustifiedAbsence(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (end == null) {
            end = start;
        }

        absenceService.handleUnjustifiedAbsence(employeeId, start, end);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/employee/{employeeId}/month")
    public ResponseEntity<List<EmployeeAbsence>> getMonthlyAbsences(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        YearMonth ym = YearMonth.of(year, month);
        return ResponseEntity.ok(absenceService.getMonthlyAbsences(employeeId, ym));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeAbsence> updateAbsence(
            @PathVariable Long id,
            @RequestBody @Valid EmployeeAbsenceRequest request
    ) {
        return ResponseEntity.ok(absenceService.updateAbsence(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAbsence(@PathVariable Long id) {
        absenceService.deleteAbsence(id);
        return ResponseEntity.noContent().build();
    }


}

