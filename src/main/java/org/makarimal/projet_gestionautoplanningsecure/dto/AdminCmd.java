package org.makarimal.projet_gestionautoplanningsecure.dto;

import java.util.List;

/* Création / update d’un admin de site */
public record AdminCmd(
        Long companyId,
        String firstName,
        String lastName,
        String email,
        String tempPassword,           // null si update
        List<Long> siteIds,            // sites qu’il gèrera
        boolean allSites
) {}

