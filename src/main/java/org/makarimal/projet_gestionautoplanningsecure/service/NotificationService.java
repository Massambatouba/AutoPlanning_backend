package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.NotificationRepository;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.Notification;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final CompanyRepository companyRepository;

    /** Méthode que tu avais déjà (conservée) */
    public void notifyCompany(Company c, String message, String icon) {
        repo.save(Notification.builder()
                .company(c)
                .message(message)
                .icon(icon)
                .build());
    }

    /** Nouvelle méthode pratique : on part d’un companyId */
    public void notifyCompanyAdmins(Long companyId, String message, String icon) {
        Company c = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company introuvable"));
        notifyCompany(c, message, icon);
    }

    /** Surcharge avec une icône par défaut */
    public void notifyCompanyAdmins(Long companyId, String message) {
        notifyCompanyAdmins(companyId, message, "bi-bell");
    }
}
