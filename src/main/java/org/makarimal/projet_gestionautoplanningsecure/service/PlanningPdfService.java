package org.makarimal.projet_gestionautoplanningsecure.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;
import org.makarimal.projet_gestionautoplanningsecure.model.ScheduleAssignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlanningPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Génère un PDF (dans un byte[]) pour le planning d’un employé donné.
     */
    public byte[] generatePdfForEmployee(Schedule schedule, List<ScheduleAssignment> assignments) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(out);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document doc = new Document(pdfDoc)) {

            // --- Logo ---
            if (schedule.getCompany().getLogoUrl() != null) {
                try {
                    ImageData imgData = ImageDataFactory.create(schedule.getCompany().getLogoUrl());
                    doc.add(new Image(imgData).scaleToFit(120, 60));
                } catch (Exception e) {
                    log.warn("Impossible de charger le logo : {}", e.getMessage());
                }
            }

            // --- Titre ---
            doc.add(new Paragraph("Planning de " + assignments.get(0).getEmployee().getFirstName()
                    + " " + assignments.get(0).getEmployee().getLastName())
                    .setBold().setFontSize(14));
            doc.add(new Paragraph("Site : " + schedule.getSite().getName()));
            doc.add(new Paragraph("Mois : "
                    + String.format("%02d/%d", schedule.getMonth(), schedule.getYear()))
                    .setMarginBottom(10));

            // --- Tableau ---
            Table table = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20}))
                    .useAllAvailableWidth();

            // en-têtes
            for (String h : List.of("Date", "Shift", "Mission", "Statut", "Heures")) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            }

            // lignes
            for (ScheduleAssignment a : assignments) {
                table.addCell(new Cell().add(new Paragraph(a.getDate().format(DATE_FMT))));
                table.addCell(new Cell().add(new Paragraph(a.getShift())));
                table.addCell(new Cell().add(new Paragraph(a.getEmployee().getPosition())));
                table.addCell(new Cell().add(new Paragraph(a.getStatus().name())));
                table.addCell(new Cell().add(new Paragraph(
                        String.valueOf(a.getDuration()) + " min")));
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Erreur génération PDF", e);
            throw new IllegalStateException("Impossible de générer le PDF", e);
        }
    }
}
