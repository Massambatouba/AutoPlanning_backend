package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SiteScheduleOverrideRequest {

    @NotNull(message = "La date de la dérogation est obligatoire")
    private LocalDate overrideDate;

    @NotNull(message = "Le nombre minimum d'agents est obligatoire")
    @Min(value = 1, message = "Le minimum d'agents doit être au moins 1")
    private Integer minEmployees;

    @NotNull(message = "Le nombre maximum d'agents est obligatoire")
    @Min(value = 1, message = "Le maximum d'agents doit être au moins 1")
    private Integer maxEmployees;

    @NotNull(message = "Le niveau d'expérience minimum est requis")
    @Min(value = 0, message = "Le niveau d'expérience minimum ne peut pas être négatif")
    private Integer minExperienceLevel;

    private boolean requiresNightShift;
    private boolean requiresWeekendCoverage;

    @NotNull(message = "La liste des compétences est obligatoire")
    @Size(min = 1, message = "Au moins une compétence doit être spécifiée")
    private List<@NotBlank(message = "Les compétences ne doivent pas être vides") String> requiredSkills;
}
