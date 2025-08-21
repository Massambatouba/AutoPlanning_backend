package org.makarimal.projet_gestionautoplanningsecure.security;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("perm")
@RequiredArgsConstructor
public class PermissionFacade {
    private final UserRepository repo;

    public boolean adminOfSite(Long siteId){
        User me = (User) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        if (me.hasRole(Role.SUPER_ADMIN) || me.hasRole(Role.ADMIN))
            return true;

        return repo.existsByIdAndManagedSites_Id(me.getId(), siteId);
    }

}
