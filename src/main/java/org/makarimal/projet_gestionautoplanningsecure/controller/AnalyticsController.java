package org.makarimal.projet_gestionautoplanningsecure.controller;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.AnalyticsSummary;
import org.makarimal.projet_gestionautoplanningsecure.dto.RevenuePoint;
import org.makarimal.projet_gestionautoplanningsecure.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/platform-admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AnalyticsController {

    private final AnalyticsService service;

    @GetMapping("/revenue")
    public List<RevenuePoint> revenue(@RequestParam(defaultValue="6") int months) {
        return service.revenueSeries(months);
    }

    @GetMapping("/summary")
    public AnalyticsSummary summary(@RequestParam(defaultValue="6") int months) {
        return service.summary(months);
    }
}

