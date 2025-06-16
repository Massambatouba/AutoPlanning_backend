package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCompanyRequest {
    // Company info
    @NotBlank private String companyName;
    private String address;
    private String phone;
    private String logoUrl;
    @Email private String email;
    private String website;
    private Long subscriptionPlanId;

    // Admin info
    @NotBlank private String adminFirstName;
    @NotBlank private String adminLastName;
    @NotBlank private String adminUsername;
    @NotBlank @Email private String adminEmail;
    @NotBlank private String adminPassword;
}

