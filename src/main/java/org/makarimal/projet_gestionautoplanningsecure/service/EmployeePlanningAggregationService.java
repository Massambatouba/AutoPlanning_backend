package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeMonthlyPlanningDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeMonthlySummaryDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.Assignment;
import org.makarimal.projet_gestionautoplanningsecure.repository.AssignmentRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.makarimal.projet_gestionautoplanningsecure.util.AuthServiceHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeePlanningAggregationService {

    private final AssignmentRepository assignmentRepo;
    private final AuthServiceHelper auth;
    private final EmployeeRepository employeeRepo;

    public EmployeeMonthlyPlanningDTO getAggregatedMonth(Long employeeId, int year, int month) {

        var ym    = YearMonth.of(year, month);
        var start = ym.atDay(1);
        var end   = ym.atEndOfMonth();

        var currentUserCompanyId = auth.getCurrentUser().getCompany().getId();

        var empCompanyId = employeeRepo.findById(employeeId)
                .map(e -> e.getCompany() != null ? e.getCompany().getId() : null)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable"));

        // garde-fou tenancy : l’admin ne peut voir que sa société
        if (!Objects.equals(empCompanyId, currentUserCompanyId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès interdit : employé d’une autre société");
        }

        // ⚠️ ICI on filtre par la société de l’EMPLOYÉ
        List<Assignment> assigns = assignmentRepo
                .findEmployeeAssignmentsInPeriodForTenant(employeeId, start, end, empCompanyId);

        // schedules uniques
        List<EmployeeMonthlyPlanningDTO.ScheduleRefDTO> schedules = assigns.stream()
                .collect(Collectors.groupingBy(a -> a.getSchedule().getId()))
                .values().stream()
                .map(list -> {
                    var any = list.get(0);
                    return EmployeeMonthlyPlanningDTO.ScheduleRefDTO.builder()
                            .scheduleId(any.getSchedule().getId())
                            .siteId(any.getSchedule().getSite().getId())
                            .siteName(any.getSchedule().getSite().getName())
                            .build();
                })
                .sorted(Comparator.comparing(EmployeeMonthlyPlanningDTO.ScheduleRefDTO::getSiteName))
                .toList();

        // calendar "YYYY-MM-DD" -> DTOs
        Map<String, List<EmployeeMonthlyPlanningDTO.AssignmentDTO>> calendar = assigns.stream()
                .map(a -> EmployeeMonthlyPlanningDTO.AssignmentDTO.builder()
                        .id(a.getId())
                        .siteId(a.getSchedule().getSite().getId())
                        .siteName(a.getSchedule().getSite().getName())
                        .date(a.getDate())           // LocalDate -> sera sérialisé "2025-08-09"
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .agentType(a.getAgentType().name())
                        .shift(a.getShift() != null ? a.getShift().name() : null)
                        .notes(a.getNotes())
                        .absence(false)
                        .absenceType(null)
                        .build())
                .collect(Collectors.groupingBy(dto -> dto.getDate().toString(), TreeMap::new, Collectors.toList()));

        return EmployeeMonthlyPlanningDTO.builder()
                .employeeId(employeeId)
                .year(year)
                .month(month)
                .scheduleIds(schedules.stream().map(EmployeeMonthlyPlanningDTO.ScheduleRefDTO::getScheduleId).toList())
                .calendar(calendar)
                .schedules(schedules)
                .build();
    }

    public List<EmployeeMonthlySummaryDTO> listMonthSummaries(Long employeeId, int year) {
        Long companyId = auth.getCurrentUser().getCompany().getId(); // ← TENANT
        List<EmployeeMonthlySummaryDTO> out = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            var ym = YearMonth.of(year, m);
            var data = assignmentRepo
                    .findEmployeeAssignmentsInPeriodForTenant(employeeId, ym.atDay(1), ym.atEndOfMonth(), companyId);

            long totalMin = data.stream().mapToLong(a -> {
                String[] s = a.getStartTime().split(":");
                String[] e = a.getEndTime().split(":");
                int sm = Integer.parseInt(s[0]) * 60 + Integer.parseInt(s[1]);
                int em = Integer.parseInt(e[0]) * 60 + Integer.parseInt(e[1]);
                if (em < sm) em += 24 * 60;
                return em - sm;
            }).sum();

            out.add(EmployeeMonthlySummaryDTO.builder()
                    .year(year).month(m)
                    .totalAssignments(data.size())
                    .totalMinutes(totalMin)
                    .build());
        }
        return out;
    }
}

