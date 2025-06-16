package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Username is required")
    private String username;

    // Ajout de la validation NotEmpty pour s'assurer que la liste de rôles n'est pas vide
    @NotEmpty(message = "At least one role is required")
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @NotBlank(message = "Company name is required")
    private String companyName;
}
