package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Builder;
import lombok.Value;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;

import java.time.LocalDateTime;

@Value
@Builder
public class CompanyDetail {
    Long id;
    String name;
    String address;
    String phone;
    String email;
    String website;
    String subscriptionStatus;
    LocalDateTime subscriptionExpiresAt;
    Integer maxEmployees;
    Integer maxSites;
    boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Owner owner;
    Plan subscriptionPlan;

    @Value @Builder
    public static class Owner {
        Long id; String firstName; String lastName; String email; String username;
    }
    @Value @Builder
    public static class Plan {
        Long id; String name; Integer maxEmployees; Integer maxSites;
    }

    public static CompanyDetail of(Company c) {
        return CompanyDetail.builder()
                .id(c.getId())
                .name(c.getName())
                .address(c.getAddress())
                .phone(c.getPhone())
                .email(c.getEmail())
                .website(c.getWebsite())
                .subscriptionStatus(c.getSubscriptionStatus() != null ? c.getSubscriptionStatus().name() : null)
                .subscriptionExpiresAt(c.getSubscriptionExpiresAt())
                .maxEmployees(c.getMaxEmployees())
                .maxSites(c.getMaxSites())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .owner(c.getOwner() == null ? null : Owner.builder()
                        .id(c.getOwner().getId())
                        .firstName(c.getOwner().getFirstName())
                        .lastName(c.getOwner().getLastName())
                        .email(c.getOwner().getEmail())
                        .username(c.getOwner().getUsername())
                        .build())
                .subscriptionPlan(c.getSubscriptionPlan() == null ? null : Plan.builder()
                        .id(c.getSubscriptionPlan().getId())
                        .name(c.getSubscriptionPlan().getName())
                        .maxEmployees(c.getSubscriptionPlan().getMaxEmployees())
                        .maxSites(c.getSubscriptionPlan().getMaxSites())
                        .build())
                .build();
    }
}
