package org.makarimal.projet_gestionautoplanningsecure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;

@Data
@AllArgsConstructor
@Builder
public class EmployeeLiteDTO {
    Long id;
    String firstName;
    String lastName;
    String email;

    public static EmployeeLiteDTO of(Employee e) {
        if (e == null) return null;
        return EmployeeLiteDTO.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .build();
    }
}
