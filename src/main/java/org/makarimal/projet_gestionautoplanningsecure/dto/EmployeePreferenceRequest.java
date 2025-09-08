package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePreferenceRequest {
    private boolean canWorkWeekends;
    private boolean canWorkNights;
    private boolean prefersDay;
    private boolean prefersNight;
    private boolean noPreference;
    private boolean canWorkWeeks;

    @NotNull(message = "Minimum hours per day is required")
    @Min(value = 1, message = "Minimum hours per day must be at least 1")
    @Max(value = 24, message = "Minimum hours per day cannot exceed 24")
    private Integer minHoursPerDay;

    @NotNull(message = "Maximum hours per day is required")
    @Min(value = 1, message = "Maximum hours per day must be at least 1")
    @Max(value = 24, message = "Maximum hours per day cannot exceed 24")
    private Integer maxHoursPerDay;

    @NotNull(message = "Minimum hours per week is required")
    @Min(value = 1, message = "Minimum hours per week must be at least 1")
    @Max(value = 168, message = "Minimum hours per week cannot exceed 168")
    private Integer minHoursPerWeek;

    @NotNull(message = "Maximum hours per week is required")
    @Min(value = 1, message = "Maximum hours per week must be at least 1")
    @Max(value = 168, message = "Maximum hours per week cannot exceed 168")
    private Integer maxHoursPerWeek;

    private Integer preferredConsecutiveDays;
    private Integer minConsecutiveDaysOff;
}

