package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Data;

@Data
public class UpdateSubscriptionRequest {
    private Long planId;
    // ex: "STARTER" | "PRO" | "ENTERPRISE" (ou le nom que tu stockes)
    private String plan;
    // ex: "ACTIVE" | "TRIAL" | "EXPIRED" | "INACTIVE"
    private String status;
}
