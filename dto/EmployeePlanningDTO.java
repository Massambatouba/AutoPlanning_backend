/* src/main/java/.../dto/EmployeePlanningDTO.java */
package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePlanningDTO {

    private Long                                  employeeId;
    private String                                employeeName;
    private Long scheduleId;
    private Map<LocalDate, List<AssignmentDTO>>   calendar;
}


