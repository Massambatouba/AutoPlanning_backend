package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractHourRequirementRequest {
    @NotNull(message = "Contract type is required")
    private Employee.ContractType contractType;

    @NotNull(message = "Minimum hours per month is required")
    @Min(value = 1, message = "Minimum hours must be at least 1")
    private Integer minimumHoursPerMonth;

    private String description;
}