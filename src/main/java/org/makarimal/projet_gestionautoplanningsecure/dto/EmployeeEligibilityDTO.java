package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EmployeeEligibilityDTO {
    boolean allowed;      // = identityOk && proOk
    boolean identityOk;   // au moins une pièce d’identité VALIDE
    boolean proOk;        // au moins un diplôme/carte pro VALIDE
    List<String> problems;

    LocalDate nextExpiry; // la plus proche
    Long daysLeft;
}
