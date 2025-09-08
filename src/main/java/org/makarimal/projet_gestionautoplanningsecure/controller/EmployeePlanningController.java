package org.makarimal.projet_gestionautoplanningsecure.controller;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeMonthlyPlanningDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeMonthlySummaryDTO;
import org.makarimal.projet_gestionautoplanningsecure.service.EmployeePlanningAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/employees/{id}/planning")
public class EmployeePlanningController {

    private final EmployeePlanningAggregationService agg;

    @GetMapping("/aggregated")
    public ResponseEntity<EmployeeMonthlyPlanningDTO> aggregated(
            @PathVariable Long id,
            @RequestParam int year,
            @RequestParam int month) {
        var dto = agg.getAggregatedMonth(id, year, month);
        System.out.println("[AGG] emp=" + id + " " + year + "-" + month
                + " schedules=" + dto.getSchedules().size()
                + " days=" + (dto.getCalendar() != null ? dto.getCalendar().size() : 0));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/summaries")
    public ResponseEntity<List<EmployeeMonthlySummaryDTO>> summaries(
            @PathVariable Long id,
            @RequestParam int year) {

        return ResponseEntity.ok(agg.listMonthSummaries(id, year));
    }
}
