package org.makarimal.projet_gestionautoplanningsecure.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeHoursSummaryDTO {
    private Long employeeId;
    private String employeeName;
    private Employee.ContractType contractType;
    private Integer requiredHours;
    private Integer actualHours;
    private Integer missingHours;
    private boolean isCompliant;
    private Double compliancePercentage;
}