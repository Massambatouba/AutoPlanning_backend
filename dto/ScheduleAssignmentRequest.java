package org.makarimal.projet_gestionautoplanningsecure.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleAssignmentRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    private Long siteId;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Agent type is required")
    private AgentType agentType;

    private String notes;

    private String shift;


}