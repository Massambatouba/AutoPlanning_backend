package org.makarimal.projet_gestionautoplanningsecure.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiteShiftTemplateAgentRequest {
    @NotNull(message = "Agent type is required")
    private AgentType agentType;
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Required count is required")
    @Min(value = 1, message = "Required count must be at least 1")
    private Integer requiredCount;

    private String notes;
}