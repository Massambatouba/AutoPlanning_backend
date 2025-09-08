package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocument;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeDocumentRepository;
import org.makarimal.projet_gestionautoplanningsecure.util.AuthServiceHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentExpiryScheduler {

    private final EmployeeDocumentRepository repo;
    private final NotificationService notificationService; // à implémenter (email, toast interne, etc.)
    private final AuthServiceHelper auth;

    // Chaque jour à 07:00
    @Scheduled(cron = "0 0 7 * * *")
    public void notifyAdmins90DaysBefore() {
        var company = auth.getCurrentUser().getCompany();
        if (company == null) return;

        LocalDate from = LocalDate.now().plusDays(89); // inclus
        LocalDate to   = LocalDate.now().plusDays(90);
        List<EmployeeDocument> expiring = repo.findExpiringBetween(from, to, company.getId());

        expiring.forEach(doc -> {
            String msg = "Le document %s de %s %s expire le %s"
                    .formatted(doc.getType().name(),
                            doc.getEmployee().getFirstName(),
                            doc.getEmployee().getLastName(),
                            doc.getExpiryDate());
            notificationService.notifyCompanyAdmins(company.getId(), "Document proche expiration", msg);
        });
    }
}

