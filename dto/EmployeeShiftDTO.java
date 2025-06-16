package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** (clé : employé, valeur : shift) – utilisé dans SitePlanningDTO */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeShiftDTO {
    private Long   employeeId;
    private String employeeName;
    private String    agentType;   // "AGENT_SIMPLE", "CHEF_POSTE", …

    private String    shiftLabel;  // "MATIN", "NUIT", …
    private String    startTime;   // "08:00"
    private String    endTime;     // "16:00"


}
