package org.makarimal.projet_gestionautoplanningsecure.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;
    @NotBlank private String newPassword;
}

