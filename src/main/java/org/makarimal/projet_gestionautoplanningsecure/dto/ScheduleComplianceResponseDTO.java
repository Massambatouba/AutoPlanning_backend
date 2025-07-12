package org.makarimal.projet_gestionautoplanningsecure.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleComplianceResponseDTO {
    private Long scheduleId;
    private String scheduleName;
    private Integer month;
    private Integer year;
    private Integer totalEmployees;
    private Integer compliantEmployees;
    private Integer nonCompliantEmployees;
    private Double overallComplianceRate;
    private List<EmployeeHoursSummaryDTO> employeeSummaries;
}
