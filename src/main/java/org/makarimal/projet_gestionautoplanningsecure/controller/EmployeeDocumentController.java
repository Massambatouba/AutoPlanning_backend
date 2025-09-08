package org.makarimal.projet_gestionautoplanningsecure.controller;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeDocumentDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeEligibilityDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocumentCategory;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocumentType;
import org.makarimal.projet_gestionautoplanningsecure.service.EmployeeDocumentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("/employees/{id}")
public class EmployeeDocumentController {
    private final EmployeeDocumentService service;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmployeeDocumentDTO create(@PathVariable Long id,
                                      @RequestPart("category") EmployeeDocumentCategory category,
                                      @RequestPart("type") EmployeeDocumentType type,
                                      @RequestPart(value="number",     required=false) String number,
                                      @RequestPart(value="expiryDate", required=false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                      @RequestPart(value="file",       required=false) MultipartFile file) {
        return service.create(id, category, type, number, expiryDate, file);
    }

    @GetMapping("/documents")
    public List<EmployeeDocumentDTO> list(@PathVariable Long id) {
        return service.list(id);
    }

    @GetMapping("/eligibility")
    public EmployeeEligibilityDTO eligibility(@PathVariable Long id) {
        return service.checkEligibility(id);
    }
}
