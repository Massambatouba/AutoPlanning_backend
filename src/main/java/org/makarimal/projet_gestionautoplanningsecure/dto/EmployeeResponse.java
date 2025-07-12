package org.makarimal.projet_gestionautoplanningsecure.dto;


import lombok.Builder;
import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;

import java.time.LocalDateTime;
import java.util.Set;

@Data @Builder
public class EmployeeResponse {

    private Long                 id;
    private Long                 companyId;
    private Long                 siteId;
    private String               siteName;
    private String               firstName;
    private String               lastName;
    private String               email;
    private String               phone;
    private String               position;
    private String               department;
    private String               employeeCode;
    private Employee.ContractType contractType;
    private Integer              maxHoursPerWeek;
    private Set<Long>            preferredSites;
    private Set<String>          skillSets;
    private Set<AgentType>       agentTypes;
    private boolean              active;
    private LocalDateTime        createdAt;
    private LocalDateTime        updatedAt;

    private String city;
    private String zipCode;
    private String adress;
    private String country;

    private PreferenceResponse preferences;

    @Data @Builder
    public static class PreferenceResponse {
        private boolean canWorkWeekends;
        private boolean canWorkWeeks;
        private boolean canWorkNights;
        private boolean prefersDay;
        private boolean prefersNight;
        private boolean noPreference;
        private Integer minHoursPerDay;
        private Integer maxHoursPerDay;
        private Integer minHoursPerWeek;
        private Integer maxHoursPerWeek;
        private Integer preferredConsecutiveDays;
        private Integer minConsecutiveDaysOff;
    }
}
