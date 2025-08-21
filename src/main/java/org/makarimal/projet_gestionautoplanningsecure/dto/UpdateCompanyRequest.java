package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Data;

@Data
public class UpdateCompanyRequest {
    private String name;
    private String address;
    private Long planId;
    private String plan;
    private String status;
    private String phone;
    private String email;
    private String website;
    private Integer maxEmployees; // optionnel: override
    private Integer maxSites;     // optionnel: override
    private Long subscriptionPlanId; // si tu veux changer de plan
}
