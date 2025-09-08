package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;
import org.makarimal.projet_gestionautoplanningsecure.model.WeeklyExceptionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Data
public class SiteWeeklyExceptionRequest {
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;

    // Optionnel: si vide -> tous les jours de la semaine compris dans la période
    private Set<DayOfWeek> daysOfWeek;

    @NotNull
    private WeeklyExceptionType type;

    // Spécification de shift (obligatoire pour ADD / REPLACE ; optionnelle pour MASK ; ignorée pour CLOSE_DAY)
    private AgentType agentType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer requiredCount;
    private Integer minExperience;
    private List<String> requiredSkills;
}

