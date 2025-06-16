package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SiteShiftTemplateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    private String description;

    @NotEmpty(message = "At least one agent type is required")
    private List<@Valid SiteShiftTemplateAgentRequest> agents;
}