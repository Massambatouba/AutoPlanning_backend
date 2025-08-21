package org.makarimal.projet_gestionautoplanningsecure.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.User;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
    private Company company;
    private boolean mustChangePassword;
    public List<String> getRoles() {
        // Sécurité : éviter l'appel à .stream() sur null
        if (user == null || user.getRoles() == null) {
            return Collections.emptyList();
        }
        return List.of(user.getRoles().get(0).name());
    }
}