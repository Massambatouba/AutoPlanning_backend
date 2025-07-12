package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;
   // @NotBlank(message = "Last site name is required")
    private String   siteName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    @NotNull  private Long siteId;

    @NotBlank(message = "Position is required")
    private String position;

    private String department;

    private String city;
    private String zipCode;
    private String adress;
    private String country;

    @NotEmpty
    private List<AgentType> agentTypes;

    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    @NotNull(message = "Contract type is required")
    private Employee.ContractType contractType;

    @NotNull(message = "Max hours per week is required")
    @Min(value = 1, message = "Max hours per week must be greater than 0")
    private Integer maxHoursPerWeek;

    private List<Long> preferredSites;
    private List<String> skillSets;

    @Valid
    @NotNull
    private PreferenceRequest preferences;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PreferenceRequest {
        private boolean canWorkWeekends;
        private boolean canWorkWeeks;
        private boolean canWorkNights;
        private boolean prefersDay;
        private boolean prefersNight;
        private boolean noPreference;
        @NotNull private Integer minHoursPerDay;
        @NotNull private Integer maxHoursPerDay;
        @NotNull private Integer minHoursPerWeek;
        @NotNull private Integer maxHoursPerWeek;
        private Integer preferredConsecutiveDays;
        private Integer minConsecutiveDaysOff;
    }
}