package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Builder;
import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;
import org.makarimal.projet_gestionautoplanningsecure.model.WeeklyExceptionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class SiteWeeklyExceptionDTO {
    private Long id;
    private Long siteId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> daysOfWeek;
    private WeeklyExceptionType type;

    private AgentType agentType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer requiredCount;
    private Integer minExperience;
    private List<String> requiredSkills;
}
