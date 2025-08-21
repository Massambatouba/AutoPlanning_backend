package org.makarimal.projet_gestionautoplanningsecure.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.SendReport;
import org.makarimal.projet_gestionautoplanningsecure.dto.SendResult;
import org.springframework.beans.factory.annotation.Value;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service @RequiredArgsConstructor
public class PlanningSendService {

    private final MailService mail;
    private final ScheduleService scheduleSrv;
    private final JwtTechTokenService techTokenSrv;
    private final PdfSnapshotService snapshot;

    @Value("${planning.front-base-url:http://localhost:4200}")
    private String frontBaseUrl;
/*
    public void send(Long scheduleId, Long empId) {
        Schedule sched = scheduleSrv.getEntity(scheduleId);
        Employee emp   = scheduleSrv.getEmployeeInSchedule(scheduleId, empId);

        String url = String.format(
                "%s/employees/print/%d?schedule=%d",
                frontBaseUrl, empId, scheduleId);

        String techJwt = techTokenSrv.buildSystemToken();
        long companyId = sched.getCompany().getId();

        byte[] pdfBytes;
        try {
            pdfBytes = snapshot.capture(url, techJwt, companyId);
        } catch (Exception pdfEx) {
            // log et rethrow ou return
            throw new RuntimeException("Échec de génération du PDF pour " + empId, pdfEx);
        }

        String subject = "Votre planning – " + sched.getSite().getName();
        String body = String.format(
                "Bonjour %s,%n%nVeuillez trouver votre planning en pièce jointe.%n" +
                        "ATTENTION : ce planning est susceptible d’être modifié.%n%nCordialement,",
                emp.getFirstName());

        try {
            mail.sendSchedulePdfToEmployee(emp.getEmail(), subject, body, pdfBytes);
        } catch (Exception mailEx) {
            throw new RuntimeException("Échec de l’envoi email à " + emp.getEmail(), mailEx);
        }
    }

 */
// PlanningSendService.java
public void send(Long scheduleId, Long empId) {
    // ⚠️ ici on charge site + company en EAGER pour ce cas d'usage
    Schedule sched = scheduleSrv.getForSending(scheduleId);

    Employee emp   = scheduleSrv.getEmployeeInSchedule(scheduleId, empId);

    String url = String.format("%s/employees/print/%d?schedule=%d",
            frontBaseUrl, empId, scheduleId);

    String techJwt  = techTokenSrv.buildSystemToken();
    long companyId  = sched.getCompany().getId();     // OK: déjà chargé

    byte[] pdfBytes = snapshot.capture(url, techJwt, companyId);

    String subject = "Votre planning – " + sched.getSite().getName(); // OK: déjà chargé
    String body = String.format(
            "Bonjour %s,%n%nVeuillez trouver votre planning en pièce jointe.%n"
                    + "ATTENTION : ce planning est susceptible d’être modifié.%n%nCordialement,",
            emp.getFirstName());

    mail.sendSchedulePdfToEmployee(emp.getEmail(), subject, body, pdfBytes);
}


    /**
     * Envoi SYNCHRONE simple (appelé par le groupe ou individuellement).
     */
    public void sendAll(Long scheduleId) {
        scheduleSrv.getAssignments(scheduleId).stream()
                .map(a -> a.getEmployee().getId())
                .distinct()
                .forEach(empId -> send(scheduleId, empId));
    }

    /**
     * Envoi GROUPÉ mais asynchrone, avec rapport détaillé.
     */
    @Async("mailExecutor")          // = nom de votre Executor déclaré dans @Configuration
    public CompletableFuture<SendReport> sendAllAsync(Long scheduleId) {

        List<SendResult> results = new ArrayList<>();

        scheduleSrv.getAssignments(scheduleId).stream()
                .map(a -> a.getEmployee().getId())
                .distinct()
                .parallel()      // ou .forEach() normal si vous préférez
                .forEach(empId -> {
                    try {
                        send(scheduleId, empId);
                        results.add(SendResult.ok(empId));
                    } catch (Exception ex) {
                        log.error("❌ Envoi planning KO pour emp {} : {}", empId, ex.getMessage());
                        results.add(SendResult.ko(empId, ex));
                    }
                });

        return CompletableFuture.completedFuture(new SendReport(results));
    }

}

