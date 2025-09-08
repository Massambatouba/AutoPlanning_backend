package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.Builder;
import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocumentCategory;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocumentType;

import java.time.LocalDate;

@Data
@Builder
public class EmployeeDocumentDTO {
    Long id;
    EmployeeDocumentCategory category;
    EmployeeDocumentType type;
    String number;
    LocalDate expiryDate;
    String fileUrl;
}