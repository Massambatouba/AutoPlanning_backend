package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;

@Data
public class WeeklyScheduleRuleRequest {
    private DayOfWeek dayOfWeek;
    private int minEmployees;
    private int maxEmployees;
    private int minExperienceLevel;
    private boolean requiresNightShift;
    private boolean requiresWeekendCoverage;
    private List<String> requiredSkills;
    @NotNull
    @Size(min = 1)
    private List<AgentScheduleRequest> agents;
    private List<WeeklyScheduleRuleRequest> rules;
}
