package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;

import java.util.List;

public record AdminCreateCmd(
        @NotNull Long   companyId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String  email,
        @NotBlank String tempPassword,
        List<Role> roles        // optionnel, default = [ADMIN]
) { }
