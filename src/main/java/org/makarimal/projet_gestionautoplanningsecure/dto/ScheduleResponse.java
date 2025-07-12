package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private Long id;
    private String name;
    private Integer month;
    private Integer year;
    private boolean published;
    private boolean validated;
    private boolean sent;
    private LocalDateTime sentAt;
    private Integer completionRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private SiteInfo site;
    private CompanyInfo company;

    private List<AssignmentDTO> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SiteInfo {
        private Long id;
        private String name;
        private String city;
        private String address;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String website;
    }
}
