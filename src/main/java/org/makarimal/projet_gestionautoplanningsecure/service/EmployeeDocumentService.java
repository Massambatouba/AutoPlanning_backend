package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeDocumentDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeEligibilityDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocument;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocumentCategory;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocumentType;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeDocumentRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.makarimal.projet_gestionautoplanningsecure.util.AuthServiceHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class EmployeeDocumentService {
    private final EmployeeDocumentRepository repo;
    private final EmployeeRepository employeeRepo;
    private final FileStorageService fileStorageService;

    @Transactional
    public EmployeeDocumentDTO create(Long employeeId,
                                      EmployeeDocumentCategory category,
                                      EmployeeDocumentType type,
                                      String number,
                                      LocalDate expiryDate,
                                      MultipartFile file) {

        Employee emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable"));

        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = fileStorageService.store(file); // à adapter
        }

        var saved = repo.save(EmployeeDocument.builder()
                .employee(emp)
                .category(category)
                .type(type)
                .number(number)
                .expiryDate(expiryDate)
                .fileUrl(fileUrl)
                .build());

        return toDto(saved);
    }

    public List<EmployeeDocumentDTO> list(Long employeeId) {
        return repo.findByEmployeeId(employeeId).stream().map(this::toDto).toList();
    }

    public EmployeeEligibilityDTO checkEligibility(Long employeeId) {
        var docs = repo.findByEmployeeId(employeeId);
        LocalDate today = LocalDate.now();

        var identityOk = docs.stream().anyMatch(d ->
                d.getCategory() == EmployeeDocumentCategory.IDENTITE &&
                        (d.getExpiryDate() == null || !d.getExpiryDate().isBefore(today)));

        var proOk = docs.stream().anyMatch(d ->
                d.getCategory() == EmployeeDocumentCategory.DIPLOME &&
                        (d.getExpiryDate() == null || !d.getExpiryDate().isBefore(today)));

        var problems = new ArrayList<String>();
        if (!identityOk) problems.add("Aucune pièce d’identité valide.");
        if (!proOk)      problems.add("Aucun diplôme/carte pro valide.");

        var next = docs.stream()
                .filter(d -> d.getExpiryDate() != null)
                .min(Comparator.comparing(EmployeeDocument::getExpiryDate))
                .map(EmployeeDocument::getExpiryDate);

        return EmployeeEligibilityDTO.builder()
                .allowed(identityOk && proOk)
                .identityOk(identityOk)
                .proOk(proOk)
                .problems(problems)
                .nextExpiry(next.orElse(null))
                .daysLeft(next.map(d -> ChronoUnit.DAYS.between(today, d)).orElse(0L))
                .build();
    }

    private EmployeeDocumentDTO toDto(EmployeeDocument d) {
        return EmployeeDocumentDTO.builder()
                .id(d.getId())
                .category(d.getCategory())
                .type(d.getType())
                .number(d.getNumber())
                .expiryDate(d.getExpiryDate())
                .fileUrl(d.getFileUrl())
                .build();
    }
}

