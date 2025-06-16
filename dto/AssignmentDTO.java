package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.*;
import org.makarimal.projet_gestionautoplanningsecure.model.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class AssignmentDTO {

    private Long        id;           // id de l’affectation
    private Long        employeeId;   // id employé
    private String      employeeName; // nom complet
    private Long        siteId;
    private LocalDate   date;         // jour
    private String      siteName;
    private String address;
    private Site site;
    private AgentType agentType;
    private String zipCode;
    private String city;
    private AbsenceType absenceType;
    private boolean isAbsence;
    private String      shift;   // “MATIN”, …
    private LocalTime   startTime;        // 08:00
    private LocalTime   endTime;          // 16:00
    private String      status;       // PENDING / CONFIRMED / …

    /* ===== mapping utilitaire ===== */
    public static AssignmentDTO of(ScheduleAssignment a) {
        Site site = a.getSite(); // peut être null
        Site fallbackSite = a.getSchedule().getSite(); // valeur de repli

        return AssignmentDTO.builder()
                .id(a.getId())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFirstName() + " " + a.getEmployee().getLastName())
                .date(a.getDate())
                .siteId(site != null ? site.getId() : fallbackSite.getId())
                .siteName(site != null ? site.getName() : fallbackSite.getName())
                .address(site != null ? site.getAddress() : fallbackSite.getAddress())
                .zipCode(site != null ? site.getZipCode() : fallbackSite.getZipCode())
                .city(site != null ? site.getCity() : fallbackSite.getCity())
                .site(site != null ? site : fallbackSite)
                .agentType(a.getAgentType())
                .shift(a.getShift())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus().name())
                .build();
    }

    public static AssignmentDTO fromAbsence(Absence absence) {
        return AssignmentDTO.builder()
                .date(absence.getDate())
                .siteName("Absence")
                .startTime(null)
                .endTime(null)
                .isAbsence(false)
                .absenceType(absence.getType())
                .build();
    }

    public static AssignmentDTO fromEmployeeAbsence(EmployeeAbsence absence, LocalDate currentDay) {
        return AssignmentDTO.builder()
                .date(currentDay) // ou adapter à chaque jour entre start/end
                .siteName(absence.getEmployee().getSite().getName())
                .startTime(null)
                .endTime(null)
                .isAbsence(true)
                .absenceType(absence.getType()) // par exemple "MALADIE"
                .build();
    }






}
