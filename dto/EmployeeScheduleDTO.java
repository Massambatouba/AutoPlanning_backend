package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeScheduleDTO {
    private Long employeeId;
    private String employeeName;
    private List<AssignmentDTO> assignments;
}
