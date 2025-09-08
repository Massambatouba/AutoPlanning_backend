package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateScheduleRequest {
    @NotNull
    private Long siteId;
    @NotBlank
    private String name;

    @NotNull private Schedule.PeriodType periodType; // MONTH | RANGE

    // pour MONTH
    private Integer month; // 1..12
    private Integer year;  // 2025...

    // pour RANGE
    private LocalDate startDate;
    private LocalDate endDate;

    @AssertTrue(message = "Pour MONTH: month/year requis. Pour RANGE: startDate/endDate requis (start<=end).")
    public boolean isConsistent() {
        if (periodType == Schedule.PeriodType.MONTH) {
            return month != null && year != null;
        }
        if (periodType == Schedule.PeriodType.RANGE) {
            return startDate != null && endDate != null && !endDate.isBefore(startDate);
        }
        return false;
    }
}

