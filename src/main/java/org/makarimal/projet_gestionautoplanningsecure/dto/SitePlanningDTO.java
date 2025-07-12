package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Planning d’un site : pour chaque date ⇒ liste (employé + shift)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SitePlanningDTO {

    private Long   siteId;
    private String siteName;
    private int    month;
    private int    year;

    /** calendrier : clé = jour, valeur = liste des shifts d’employés */
    private Map<LocalDate, List<EmployeeShiftDTO>> calendar;

    private List<EmployeePlanningDTO> employees;
}
