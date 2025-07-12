package org.makarimal.projet_gestionautoplanningsecure.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SubscriptionPlanRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.SubscriptionPlan;
import org.makarimal.projet_gestionautoplanningsecure.repository.SubscriptionPlanRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SubscriptionPlan createPlan(SubscriptionPlanRequest request) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .maxEmployees(request.getMaxEmployees())
                .maxSites(request.getMaxSites())
                .price(request.getPrice())
                .durationMonths(request.getDurationMonths())
                .isActive(true)
                .build();

        return subscriptionPlanRepository.save(plan);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAll();
    }

    public List<SubscriptionPlan> getActivePlans() {
        return subscriptionPlanRepository.findByIsActiveTrue();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SubscriptionPlan getPlan(Long id) {
        return subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription plan not found"));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SubscriptionPlan updatePlan(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = getPlan(id);

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setMaxEmployees(request.getMaxEmployees());
        plan.setMaxSites(request.getMaxSites());
        plan.setPrice(request.getPrice());
        plan.setDurationMonths(request.getDurationMonths());

        return subscriptionPlanRepository.save(plan);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public SubscriptionPlan togglePlanStatus(Long id) {
        SubscriptionPlan plan = getPlan(id);
        plan.setActive(!plan.isActive());
        return subscriptionPlanRepository.save(plan);
    }
}
