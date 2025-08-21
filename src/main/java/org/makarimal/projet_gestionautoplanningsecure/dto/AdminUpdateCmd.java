package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.NotBlank;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;

import java.util.List;

public record AdminUpdateCmd(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String  phone,
        String  position,
        List<Role> roles
) { }