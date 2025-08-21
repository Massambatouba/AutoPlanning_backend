package org.makarimal.projet_gestionautoplanningsecure.dto;

import java.util.List;

public record UserSummary(Long id, String email, List<String> roles, boolean mustChangePassword) {}
