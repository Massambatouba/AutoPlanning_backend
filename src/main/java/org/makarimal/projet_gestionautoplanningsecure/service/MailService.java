package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    /**
     * Envoie un email avec le PDF en pièce jointe.
     */
    public void sendSchedulePdfToEmployee(String toEmail,
                                          String subject,
                                          String text,
                                          byte[] pdfData) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(text);
            helper.addAttachment("planning.pdf",
                    new ByteArrayResource(pdfData),
                    "application/pdf");
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("Impossible d’envoyer le mail", e);
        }
    }
}
