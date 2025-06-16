package org.makarimal.projet_gestionautoplanningsecure.dto;


import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class CompleteEmployeeRequest {
    @Valid
    private EmployeeRequest employee;

    @Valid
    private List<EmployeeAvailabilityRequest> availability;

    @Valid
    private EmployeePreferenceRequest preferences;
}
