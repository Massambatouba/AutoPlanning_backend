package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;

@Data
public class AgentScheduleRequest {
    private AgentType agentType;
    private String startTime;
    private String endTime;
    private int requiredCount;
    private String notes;
}

