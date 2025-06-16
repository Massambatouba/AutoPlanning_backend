package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Min;
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
public class SiteScheduleRuleRequest {
    @NotNull(message = "Working days are required")
    private List<DayOfWeek> workingDays;

    @NotNull(message = "Minimum employees per day is required")
    @Min(value = 1, message = "Minimum employees per day must be at least 1")
    private Integer minEmployeesPerDay;

    @NotNull(message = "Maximum employees per day is required")
    @Min(value = 1, message = "Maximum employees per day must be at least 1")
    private Integer maxEmployeesPerDay;

    private boolean requiresNightShift;
    private boolean requiresWeekendCoverage;

    @NotNull(message = "Minimum experience level is required")
    @Min(value = 0, message = "Minimum experience level cannot be negative")
    private Integer minExperienceLevel;

    private List<String> requiredSkills;
}