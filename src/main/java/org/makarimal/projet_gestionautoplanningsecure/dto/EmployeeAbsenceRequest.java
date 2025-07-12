package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.model.AbsenceType;

import java.time.LocalDate;

@Data
public class EmployeeAbsenceRequest {
    private Long employeeId;
    private AbsenceType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}