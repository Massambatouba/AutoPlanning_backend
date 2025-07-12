package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SiteResponse  {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String zipCode;
    private String country;
    private String phone;
    private String email;
    private String managerName;
    private String managerEmail;
    private String managerPhone;
    private String clientRequests;
    private boolean active;
}
