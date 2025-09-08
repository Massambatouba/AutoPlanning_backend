package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeMonthlyPlanningDTO {
    private Long employeeId;
    private int year;
    private int month;
    private List<Long> scheduleIds;
    private Map<String, List<AssignmentDTO>> calendar;
    private List<ScheduleRefDTO> schedules;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AssignmentDTO {
        private Long id;
        private Long siteId;
        private String siteName;
        private LocalDate date;
        private String startTime; // "HH:mm"
        private String endTime;   // "HH:mm"
        private String agentType;
        private String shift;
        private String notes;
        private boolean absence;
        private String absenceType;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ScheduleRefDTO {
        private Long scheduleId;
        private Long siteId;
        private String siteName;
    }


}
