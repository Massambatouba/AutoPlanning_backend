package org.makarimal.projet_gestionautoplanningsecure.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiteShiftRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Required employees count is required")
    @Min(value = 1, message = "Required employees count must be at least 1")
    private Integer requiredEmployees;

    @NotNull(message = "Minimum experience is required")
    @Min(value = 0, message = "Minimum experience cannot be negative")
    private Integer minExperience;

    private List<String> requiredSkills;

    @NotNull(message = "Required agent types are required")
    private List<AgentType> requiredAgentTypes;
}