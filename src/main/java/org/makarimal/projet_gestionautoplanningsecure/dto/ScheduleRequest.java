package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleRequest {
    private String name;

    @NotNull(message = "Site ID is required")
    private Long siteId;

    private Schedule.PeriodType periodType;


    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;


    @Min(value = 2024, message = "Year must be 2024 or later")
    private Integer year;
}