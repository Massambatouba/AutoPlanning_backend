package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.SubscriptionPlan;
import org.makarimal.projet_gestionautoplanningsecure.service.CompanyService;
import org.makarimal.projet_gestionautoplanningsecure.service.DashboardService;
import org.makarimal.projet_gestionautoplanningsecure.service.SubscriptionPlanService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")      // ← UNE ligne protège tout
@RequiredArgsConstructor
public class PlatformAdminController {

    private final CompanyService companySrv;
    private final SubscriptionPlanService planSrv;
    private final DashboardService dashSrv;

    /* ① Stats globales -------------------------------------------------- */
    @GetMapping("/stats")
    public PlatformStats stats() {
        return dashSrv.platformStats();
    }

    /* ② Entreprises ----------------------------------------------------- */
    @GetMapping("/companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<CompanyOverview> companies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)  String q,
            @RequestParam(required = false)  String status) {

        return companySrv.search(page, size, q, status)
                .map(CompanyOverview::of);
    }

    @GetMapping("/companies/{id}")
    public CompanyDetail company(@PathVariable Long id) {
        var company = companySrv.getCompany(id);
        return CompanyDetail.of(company);
    }

    @PutMapping("/companies/{id}/toggle-status")
    public Company toggleCompany(@PathVariable Long id) {
        return companySrv.toggleCompanyStatus(id);
    }

    /* ③ Plans d’abonnement --------------------------------------------- */
    @GetMapping("/subscription-plans")
    public List<SubscriptionPlan> allPlans() {
        return planSrv.getAllPlans();
    }

    @PostMapping("/subscription-plans")
    public SubscriptionPlan createPlan(@RequestBody @Valid SubscriptionPlanRequest req){
        return planSrv.createPlan(req);
    }

    @PutMapping("/subscription-plans/{id}")
    public SubscriptionPlan updatePlan(@PathVariable Long id, @RequestBody @Valid SubscriptionPlanRequest req) {
        return planSrv.updatePlan(id, req);
    }

    /* ④ Revenus --------------------------------------------------------- */
    @GetMapping("/revenues")
    public List<RevenueData> revenues(@RequestParam(defaultValue="6") int months){
        return dashSrv.revenueLastMonths(months);
    }

    @GetMapping("/companies_id/{id}")
    public CompanyOverview companyById(@PathVariable Long id) {
        return companySrv.getOverview(id);
    }



    @PatchMapping("/companies/{id}/toggle-status")
    public CompanyOverview toggle(@PathVariable Long id, @RequestBody ToggleActiveRequest req) {
        return companySrv.setActive(id, req.isActive());
    }

    @PutMapping("/companies/{id}/subscription")
    public CompanyOverview updateCompanySubscription(@PathVariable Long id,
                                                     @RequestBody UpdateSubscriptionRequest req) {
        return companySrv.updateSubscription(id, req);
    }

    @DeleteMapping("/companies/{id}")
    public void delete(@PathVariable Long id) {
        companySrv.deleteCompany(id);
    }


}
