/* src/main/java/.../dto/EmployeePlanningDTO.java */
package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.*;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class EmployeePlanningDTO {

    private Long employeeId;
    private String employeeName;
    private int    month;
    private int year;
    private Long scheduleId;
    private String responsableName;
    private Map<LocalDate, List<AssignmentDTO>> calendar;

    public EmployeePlanningDTO(Long employeeId, String employeeName) {
        this.employeeId   = employeeId;
        this.employeeName = employeeName;
        this.responsableName = responsableName;
    }

    public EmployeePlanningDTO(Long employeeId,
                               String employeeName,
                               String responsableName) {
        this.employeeId      = employeeId;
        this.employeeName    = employeeName;
        this.responsableName = responsableName;
    }
    public static EmployeePlanningDTO of(Employee e) {
        if (e == null) return null;
        return EmployeePlanningDTO.builder()
                .employeeId(e.getId())
                .employeeName(e.getFirstName())
                .build();
    }


}