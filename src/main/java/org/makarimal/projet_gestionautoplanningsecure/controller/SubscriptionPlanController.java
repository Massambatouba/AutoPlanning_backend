package org.makarimal.projet_gestionautoplanningsecure.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SubscriptionPlanRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.SubscriptionPlan;
import org.makarimal.projet_gestionautoplanningsecure.service.SubscriptionPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {
    private final SubscriptionPlanService subscriptionPlanService;

    @PostMapping
    public ResponseEntity<SubscriptionPlan> createPlan(@Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(subscriptionPlanService.createPlan(request));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        return ResponseEntity.ok(subscriptionPlanService.getAllPlans());
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getActivePlans() {
        return ResponseEntity.ok(subscriptionPlanService.getActivePlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionPlanService.getPlan(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanRequest request
    ) {
        return ResponseEntity.ok(subscriptionPlanService.updatePlan(id, request));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<SubscriptionPlan> togglePlanStatus(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionPlanService.togglePlanStatus(id));
    }
}
