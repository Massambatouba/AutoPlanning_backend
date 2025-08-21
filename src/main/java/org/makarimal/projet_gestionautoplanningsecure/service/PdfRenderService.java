package org.makarimal.projet_gestionautoplanningsecure.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeePlanningDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PdfRenderService {

    private final SpringTemplateEngine thymeleaf;

    public byte[] build(Employee emp,
                        Schedule sched,
                        EmployeePlanningDTO planning,
                        Company company) {

        var ctx = new Context(Locale.FRENCH);
        ctx.setVariable("emp",      emp);
        ctx.setVariable("planning", planning);
        ctx.setVariable("company",  company);

        // 1. HTML final
        String html = thymeleaf.process("schedule-pdf", ctx);

        // 2. PDF
        try (var out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null)
                    .toStream(out)
                    .run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("PDF generation failed", ex);
        }
    }
}

