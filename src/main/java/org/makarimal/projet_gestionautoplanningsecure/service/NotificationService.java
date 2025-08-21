package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.NotificationRepository;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.Notification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final NotificationRepository repo;

    public void notifyCompany(Company c, String message, String icon) {
        repo.save(Notification.builder()
                .company(c)
                .message(message)
                .icon(icon)
                .build());
    }
}

