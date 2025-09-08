package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeMonthlySummaryDTO {
    private int year;
    private int month;
    private long totalAssignments;
    private long totalMinutes;
}

