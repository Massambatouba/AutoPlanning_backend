package org.makarimal.projet_gestionautoplanningsecure.controller;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.DashboardStatsDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.NotificationDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.NotificationRepository;
import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ScheduleRepository schedRepo;
    private final EmployeeRepository empRepo;
    private final SiteRepository siteRepo;
    private final NotificationRepository notifRepo;

    /** ① KPI globaux pour l’entreprise courante */
// DashboardController.java
    @GetMapping("/stats")
    public DashboardStatsDTO stats(@AuthenticationPrincipal User me) {
        Long cid = me.getCompanyId();

        long schedCount = schedRepo.countByCompany_Id(cid);
        long empCount   = empRepo.countByCompany_Id(cid);
        long siteCount  = siteRepo.countByCompany_Id(cid);
        double completion = schedRepo.findAverageCompletionRate(cid);

        return new DashboardStatsDTO(schedCount, empCount, siteCount, completion);
    }


    /** ② 5 derniers plannings (par défaut) */
    @GetMapping("/recent-schedules")
    public List<Schedule> recent(
            @AuthenticationPrincipal User me,
            @RequestParam(defaultValue = "5") int limit) {

        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        Pageable page = PageRequest.of(0, limit);

        return schedRepo
                .findByCompany_IdOrderByCreatedAtDesc(me.getCompanyId(), page);
    }

    @GetMapping("/notifications")
    public List<NotificationDTO> notifications(
            @AuthenticationPrincipal User me,
            @RequestParam(defaultValue = "10") int limit) {

        Pageable p = PageRequest.of(0, Math.max(1, limit));
        return notifRepo.findRecent(me.getCompanyId(), p)
                .stream()
                .map(NotificationDTO::of)
                .toList();
    }
}
