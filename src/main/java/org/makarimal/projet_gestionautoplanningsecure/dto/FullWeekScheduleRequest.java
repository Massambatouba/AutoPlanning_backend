package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.dto.WeeklyScheduleRuleRequest;

import java.util.List;

@Data
public class FullWeekScheduleRequest {
    private List<WeeklyScheduleRuleRequest> rules;  // un par jour
}
